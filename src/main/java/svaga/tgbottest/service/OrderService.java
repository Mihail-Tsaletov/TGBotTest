package svaga.tgbottest.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import svaga.tgbottest.model.Doctor;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.model.User;
import svaga.tgbottest.repository.DoctorRepository;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class OrderService {
    private final static Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final ToothService toothService;

    public OrderService(OrderRepository orderRepository, DoctorRepository doctorRepository, UserRepository userRepository, TelegramService telegramService, ToothService toothService) {
        this.orderRepository = orderRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.telegramService = telegramService;
        this.toothService = toothService;
    }

    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus("pending");
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("cancelled");
        orderRepository.save(order);

        String message = "❌ Ваша запись отменена менеджером.\nСвяжитесь с нами для новой записи.";
        telegramService.sendMessage(order.getUser().getChatId(), message);
    }

    public void confirmOrder(Long orderId, Long doctorId, LocalDateTime appointmentDate) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Врач не найден"));

        order.setDoctor(doctor);
        order.setAppointmentDate(appointmentDate);
        order.setStatus("confirmed");
        order.setConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        String message = String.format(
                "✅ Ваша запись подтверждена!\n\n" +
                        "Врач: %s\n" +
                        "Дата и время: %s\n" +
                        "Ждём вас в клинике!",
                doctor.getFullName(),
                appointmentDate.format(DATE_FORMATTER)
        );

        String fileId = doctor.getPhotoUrl();
        if (fileId != null && !fileId.isBlank()) {
            telegramService.sendPhoto(order.getUser().getChatId(), fileId, message);
        } else {
            telegramService.sendMessage(order.getUser().getChatId(), message);
        }
    }

    @Transactional
    public void completeOrder(Long orderId, BigDecimal fullPrice, Integer usedToothPoints,
                              boolean cleaning, boolean whitening, boolean extraction) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();

        if (!"confirmed".equals(order.getStatus())) {
            throw new IllegalStateException("Можно завершить только подтверждённую запись");
        }

        if (fullPrice == null || fullPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        // Рассчитываем текущий баланс зубиков (активные, не сгоревшие)
        int currentBalance = toothService.getActiveBalance(user);

        // Проверка использованных зубиков
        int used = (usedToothPoints != null && usedToothPoints > 0) ? usedToothPoints : 0;
        if (used > currentBalance) {
            throw new IllegalArgumentException("Недостаточно зубиков на балансе");
        }
        if (used > fullPrice.intValue()) {
            throw new IllegalArgumentException("Нельзя использовать зубиков больше, чем сумма");
        }

        // Сохраняем полную сумму
        order.setPrice(fullPrice);
        order.setStatus("completed");
        order.setCompletedAt(LocalDateTime.now());

        // Финальная сумма к оплате деньгами
        BigDecimal cashPaid = fullPrice.subtract(new BigDecimal(used));

        // Обновляем статистику
        user.setTotalSpent(user.getTotalSpent().add(fullPrice));

        if (used > 0) {
            toothService.spendTooth(user, used, "Оплата приёма №" + orderId);
        }

        // Начисляем 1% от полной суммы, только если не тратили зубики на эту оплату
        if (used == 0) {
            int earned = (int) Math.round(fullPrice.doubleValue() * 0.01);
            if (earned > 0) {
                toothService.earnTooth(user, earned, "Начисление 1% за приём №" + orderId);
            }
        }

        orderRepository.save(order);
        userRepository.save(user);

        // Уведомление в Telegram
        int newBalance = toothService.getActiveBalance(user);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String message = String.format(
                "✅ Приём завершён!\n\n" +
                        "Врач: %s\n" +
                        "Дата: %s\n" +
                        "Стоимость услуги: %.2f ₽\n",
                order.getDoctor().getFullName(),
                order.getAppointmentDate().format(fmt),
                fullPrice
        );

        if (used > 0) {
            message += String.format(
                    "Оплачено зубиками: %d\n" +
                            "К оплате наличными/картой: %.2f ₽\n",
                    used,
                    cashPaid
            );
        }

        if (used == 0) {
            int earned = (int) Math.round(fullPrice.doubleValue() * 0.01);
            message += String.format(
                    "Начислено зубиков: +%d 🎉\n",
                    earned
            );
        }

        message += String.format(
                "Текущий баланс зубиков: %d 🦷\n\n" +
                        "Спасибо за визит!",
                newBalance
        );

        telegramService.sendMessage(order.getUser().getChatId(), message);
        // Вторые сообщения с рекомендациями
        if (cleaning) {
            telegramService.sendMessage(user.getChatId(),
                    "🦷 После профессиональной чистки:\n\n" +
                            "• В первые 2–3 часа воздержитесь от еды и напитков (особенно красящих: кофе, чай, красное вино).\n" +
                            "• В течение суток не курите и не ешьте сильно красящие продукты.\n" +
                            "• Чистите зубы мягкой щёткой, чтобы не травмировать эмаль.\n" +
                            "• Через 1–2 недели эффект будет максимальным!\n\n" +
                            "Спасибо за заботу о улыбке! 😊");
        }

        if (whitening) {
            telegramService.sendMessage(user.getChatId(),
                    "✨ После отбеливания:\n\n" +
                            "• В течение 48 часов соблюдайте \"белую диету\": избегайте красящих продуктов (кофе, чай, красное вино, свёкла, ягоды, соусы).\n" +
                            "• Не курите.\n" +
                            "• Пейте через трубочку.\n" +
                            "• Используйте зубную пасту для чувствительных зубов, если есть дискомфорт.\n" +
                            "• Эффект сохранится дольше при регулярной гигиене!\n\n" +
                            "Ваша улыбка стала ещё ярче! 🌟");
        }

        if (extraction) {
            telegramService.sendMessage(user.getChatId(),
                    "🦷 После удаления зуба:\n\n" +
                            "• В первые 2–3 часа не ешьте и не пейте горячее.\n" +
                            "• Не полощите рот интенсивно в первые сутки (чтобы не вымыть сгусток).\n" +
                            "• Прикладывайте холод к щеке по 10–15 минут с перерывами.\n" +
                            "• Не курите и не употребляйте алкоголь 2–3 дня.\n" +
                            "• Принимайте назначенные препараты.\n" +
                            "• Если боль или отёк усиливаются — звоните нам!\n\n" +
                            "Желаем быстрого восстановления! ❤️");
        }
    }

    public void rescheduleAppointment(Long orderId, Long doctorId, LocalDateTime newAppointmentDate) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (!"confirmed".equals(order.getStatus())) {
            throw new IllegalStateException("Переносить можно только подтверждённые записи");
        }

        LocalDateTime oldDate = order.getAppointmentDate();
        String oldDoctorName = order.getDoctor() != null ? order.getDoctor().getFullName() : "Любой врач";

        order.setAppointmentDate(newAppointmentDate);

        if (doctorId != null) {
            Doctor newDoctor = doctorRepository.findById(doctorId).orElseThrow();
            order.setDoctor(newDoctor);
        }

        orderRepository.save(order);

        String newDoctorName = order.getDoctor() != null ? order.getDoctor().getFullName() : "Любой врач";

        String message = String.format(
                "📅 Ваша запись перенесена!\n\n" +
                        "Было:\n" +
                        "Врач: %s\n" +
                        "Дата и время: %s\n\n" +
                        "Стало:\n" +
                        "Врач: %s\n" +
                        "Новая дата и время: %s\n\n" +
                        "Если время неудобно — свяжитесь с нами!",
                oldDoctorName,
                oldDate.format(DATE_FORMATTER),
                newDoctorName,
                newAppointmentDate.format(DATE_FORMATTER)
        );

        String fileId = order.getDoctor().getPhotoUrl();
        if (fileId != null && !fileId.isBlank()) {
            telegramService.sendPhoto(order.getUser().getChatId(), fileId, message);
        } else {
            telegramService.sendMessage(order.getUser().getChatId(), message);
        }
    }
}
