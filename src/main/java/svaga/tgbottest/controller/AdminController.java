package svaga.tgbottest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import svaga.tgbottest.service.OrderService;
import svaga.tgbottest.service.TelegramService;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String showPendingOrders(Model model) {
        model.addAttribute("orders", orderService.getPendingOrders());
        return "/orders";
    }

    @PostMapping("/order/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/admin/orders";
    }

    @PostMapping("/order/{id}/confirm")
    public String confirmOrder(
            @PathVariable Long id,
            @RequestParam String doctor,
            @RequestParam("appointmentDate") String dateTimeStr,
            @RequestParam BigDecimal price) throws ParseException {

        LocalDateTime appointmentDate = LocalDateTime.parse(dateTimeStr);

        orderService.confirmOrder(id, doctor, appointmentDate, price);
        return "redirect:/admin/orders";
    }
}
