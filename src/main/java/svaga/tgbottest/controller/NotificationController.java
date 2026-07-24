package svaga.tgbottest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.repository.OrderRepository;

import java.util.Map;

@RestController
@RequestMapping("/tg/internal")
public class NotificationController {

    private final static Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${internal.notify.secret}")
    private String internalSecret;

    public NotificationController(OrderRepository orderRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/new-order/{orderId}")
    public ResponseEntity<String> notifyNewOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Internal-Secret") String secret) {

        log.info("Получен запрос на уведомление о новой заявке #{}", orderId);

        if (!internalSecret.equals(secret)) {
            log.warn("Неверный секрет во внутреннем уведомлении для order #{}", orderId);
            return ResponseEntity.status(403).body("forbidden");
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order #{} не найден при попытке отправить уведомление", orderId);
            return ResponseEntity.status(404).body("order not found");
        }

        // Более информативное уведомление
        Map<String, Object> payload = Map.of(
                "orderId", order.getId(),
                "createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "unknown",
                "userId", order.getUser() != null ? order.getUser().getId() : null
        );

        messagingTemplate.convertAndSend("/topic/new-orders", payload);
        log.info("Уведомление о новой заявке #{} отправлено в /topic/new-orders", orderId);

        return ResponseEntity.ok("sent");
    }
}