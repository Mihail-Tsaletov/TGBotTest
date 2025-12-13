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
    }

    public void confirmOrder(Long orderId, String doctor, LocalDateTime appointmentDate, BigDecimal price) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();

        order.setDoctor(doctor);
        order.setAppointmentDate(appointmentDate);
        order.setPrice(price);
        order.setStatus("confirmed");
        order.setConfirmedAt(LocalDateTime.now());

        // Обновляем сумму и скидку пользователя
        BigDecimal newTotal = user.getTotalSpent().add(price);
        user.setTotalSpent(newTotal);

        int newDiscount = calculateDiscount(newTotal);
        user.setDiscountPercent(newDiscount);

        orderRepository.save(order);
        userRepository.save(user);

        // Уведомление в Telegram
        String message = String.format(
                "✅ Ваша заявка подтверждена!\n\n" +
                        "Врач: %s\n" +
                        "Дата и время: %s\n" +
                        "Стоимость: %.2f ₽\n" +
                        "Ваша текущая скидка: %d%%",
                doctor,
                appointmentDate.format(DATE_FORMATTER),
                price,
                newDiscount
        );

        telegramService.sendMessage(user.getChatId(), message);
    }

    private int calculateDiscount(BigDecimal total) {
        if (total.compareTo(new BigDecimal("50000")) >= 0) return 15;
        if (total.compareTo(new BigDecimal("30000")) >= 0) return 10;
        if (total.compareTo(new BigDecimal("10000")) >= 0) return 5;
        return 0;
    }
}
