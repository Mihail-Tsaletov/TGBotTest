package svaga.tgbottest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.service.OrderService;
import svaga.tgbottest.service.TelegramService;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public AdminController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
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
            @RequestParam("appointmentDate") String dateTimeStr)
            throws ParseException {
        LocalDateTime appointmentDate = LocalDateTime.parse(dateTimeStr);

        orderService.confirmOrder(id, doctor, appointmentDate);
        return "redirect:/admin/orders";
    }

    @GetMapping("/completed")
    public String showCompletedPage(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        LocalDate searchDate = (date != null) ? date : LocalDate.now();

        List<Order> orders;
        if (phone != null && !phone.isBlank()) {
            String phonePattern = "%" + phone.trim() + "%";
            orders = orderRepository.findConfirmedByAppointmentDateAndPhone(searchDate, phonePattern);
        } else {
            orders = orderRepository.findConfirmedByAppointmentDate(searchDate);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("searchDate", searchDate);
        model.addAttribute("phone", phone);
        return "/completed";
    }

    @PostMapping("/complete/{id}")
    public String completeOrder(
            @PathVariable Long id,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, defaultValue = "0") Integer usedToothPoints) {

        orderService.completeOrder(id, price, usedToothPoints);

        // Возврат на ту же страницу с параметрами поиска
        String redirect = "redirect:/admin/completed";
        if (phone != null && !phone.isBlank()) redirect += "?phone=" + phone;
        if (date != null) redirect += (redirect.contains("?") ? "&" : "?") + "date=" + date;
        return redirect;
    }

    @PostMapping("/reschedule/{id}")
    public String rescheduleOrder(
            @PathVariable Long id,
            @RequestParam String doctor,
            @RequestParam("newAppointmentDate") String newDateTimeStr,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LocalDate date) throws ParseException {

        LocalDateTime newAppointmentDate = LocalDateTime.parse(newDateTimeStr);

        orderService.rescheduleAppointment(id, doctor.isBlank() ? null : doctor, newAppointmentDate);

        // Редирект с сохранением фильтров
        String redirect = "redirect:/admin/completed";
        if (phone != null && !phone.isBlank()) redirect += "?phone=" + phone;
        if (date != null) redirect += (redirect.contains("?") ? "&" : "?") + "date=" + date;
        return redirect;
    }
}
