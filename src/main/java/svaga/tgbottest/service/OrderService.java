package svaga.tgbottest.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.model.User;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class OrderService {
    private final static Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, TelegramService telegramService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.telegramService = telegramService;
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

    public void confirmOrder(Long orderId, String doctor, LocalDateTime appointmentDate) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();

        order.setDoctor(doctor);
        order.setAppointmentDate(appointmentDate);
        order.setStatus("confirmed");
        order.setConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        String message = String.format(
                "✅ Ваша запись подтверждена!\n\n" +
                        "Врач: %s\n" +
                        "Дата и время: %s\n" +
                        "Приходите вовремя!",
                doctor,
                appointmentDate.format(DATE_FORMATTER)
        );
        telegramService.sendMessage(order.getUser().getChatId(), message);
    }

    @Transactional
    public void completeOrder(Long orderId, BigDecimal fullPrice, Integer usedToothPoints) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();

        if (!"confirmed".equals(order.getStatus())) {
            throw new IllegalStateException("Можно завершить только подтверждённую запись");
        }

        if (fullPrice == null || fullPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        // Проверка использованных зубиков
        int used = (usedToothPoints != null && usedToothPoints > 0) ? usedToothPoints : 0;
        if (used > user.getToothPoints()) {
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

        // Списываем использованные зубики
        int newBalance = user.getToothPoints() - used;

        // Начисляем 5% от полной суммы, только если не тратили зубики на эту оплату
        if (used == 0) {
            int earned = (int) Math.round(fullPrice.doubleValue() * 0.05);
            newBalance += earned;
        }

        user.setToothPoints(newBalance);

        orderRepository.save(order);
        userRepository.save(user);

        // Уведомление в Telegram
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String message = String.format(
                "✅ Приём завершён!\n\n" +
                        "Врач: %s\n" +
                        "Дата: %s\n" +
                        "Стоимость услуги: %.2f ₽\n",
                order.getDoctor(),
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
            int earned = (int) Math.round(fullPrice.doubleValue() * 0.05);
            message += String.format(
                    "Начислено зубиков: +%d 🎉\n",
                    earned
            );
        }

        message += String.format(
                "Баланс зубиков: %d\n\n" +
                        "Спасибо за визит!",
                newBalance
        );

        telegramService.sendMessage(user.getChatId(), message);
    }

    public void rescheduleAppointment(Long orderId, String doctor, LocalDateTime newAppointmentDate) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (!"confirmed".equals(order.getStatus())) {
            throw new IllegalStateException("Переносить можно только подтверждённые записи");
        }

        LocalDateTime oldDate = order.getAppointmentDate();
        String oldDoctor = order.getDoctor();

        order.setAppointmentDate(newAppointmentDate);
        if (doctor != null && !doctor.isBlank()) {
            order.setDoctor(doctor);
        }

        orderRepository.save(order);

        // Уведомление пациенту о переносе
        String message = String.format(
                "📅 Ваша запись перенесена!\n\n" +
                        "Было:\n" +
                        "Врач: %s\n" +
                        "Дата и время: %s\n\n" +
                        "Стало:\n" +
                        "Врач: %s\n" +
                        "Новая дата и время: %s\n\n",
                oldDoctor != null ? oldDoctor : "Любой врач",
                oldDate.format(DATE_FORMATTER),
                order.getDoctor(),
                newAppointmentDate.format(DATE_FORMATTER)
        );

        telegramService.sendMessage(order.getUser().getChatId(), message);
    }

    private int calculateDiscount(BigDecimal total) {
        if (total.compareTo(new BigDecimal("50000")) >= 0) return 15;
        if (total.compareTo(new BigDecimal("30000")) >= 0) return 10;
        if (total.compareTo(new BigDecimal("10000")) >= 0) return 5;
        return 0;
    }
}
