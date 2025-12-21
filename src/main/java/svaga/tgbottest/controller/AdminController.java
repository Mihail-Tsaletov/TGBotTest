package svaga.tgbottest.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import svaga.tgbottest.DTO.OrderView;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.repository.DoctorRepository;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.service.OrderService;
import svaga.tgbottest.service.ToothService;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final DoctorRepository doctorRepository;
    private final ToothService toothService;

    public AdminController(OrderService orderService, OrderRepository orderRepository, DoctorRepository doctorRepository, ToothService toothService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.doctorRepository = doctorRepository;
        this.toothService = toothService;
    }

    @GetMapping("/orders")
    public String showPendingOrders(Model model) {
        List<Order> orders = orderService.getPendingOrders();
        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(order, toothService.getActiveBalance(order.getUser())))
                .toList();

        model.addAttribute("orders", orderViews);
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "orders";
    }

    @GetMapping("/orders/fragment")
    public String ordersFragment(Model model) {
        List<Order> orders = orderService.getPendingOrders();

        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(order, toothService.getActiveBalance(order.getUser())))
                .toList();

        model.addAttribute("orders", orderViews);
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "orders :: ordersListFragment";
    }

    @PostMapping("/order/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/admin/orders";
    }

    @PostMapping("/order/{id}/confirm")
    public String confirmOrder(
            @PathVariable Long id,
            @RequestParam Long doctorId,
            @RequestParam("appointmentDate") String dateTimeStr)
            throws ParseException {
        LocalDateTime appointmentDate = LocalDateTime.parse(dateTimeStr);
        orderService.confirmOrder(id, doctorId, appointmentDate);
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

        // Преобразуем в DTO с рассчитанным балансом зубиков
        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(order, toothService.getActiveBalance(order.getUser())))
                .toList();

        model.addAttribute("orders", orderViews);
        model.addAttribute("searchDate", searchDate);
        model.addAttribute("phone", phone);
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "completed";
    }

    @PostMapping("/complete/{id}")
    public String completeOrder(
            @PathVariable Long id,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Boolean cleaning,
            @RequestParam(required = false) Boolean whitening,
            @RequestParam(required = false) Boolean extraction,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, defaultValue = "0") Integer usedToothPoints) {

        orderService.completeOrder(id, price, usedToothPoints,
                Boolean.TRUE.equals(cleaning),
                Boolean.TRUE.equals(whitening),
                Boolean.TRUE.equals(extraction));

        // Возврат на ту же страницу с параметрами поиска
        String redirect = "redirect:/admin/completed";
        if (phone != null && !phone.isBlank()) redirect += "?phone=" + phone;
        if (date != null) redirect += (redirect.contains("?") ? "&" : "?") + "date=" + date;
        return redirect;
    }

    @PostMapping("/reschedule/{id}")
    public String rescheduleOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Long doctorId,
            @RequestParam("newAppointmentDate") String newDateTimeStr,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LocalDate date) throws ParseException {

        LocalDateTime newAppointmentDate = LocalDateTime.parse(newDateTimeStr);

        orderService.rescheduleAppointment(id, doctorId, newAppointmentDate);
        // Редирект с сохранением фильтров
        String redirect = "redirect:/admin/completed";
        if (phone != null && !phone.isBlank()) redirect += "?phone=" + phone;
        if (date != null) redirect += (redirect.contains("?") ? "&" : "?") + "date=" + date;
        return redirect;
    }
}
