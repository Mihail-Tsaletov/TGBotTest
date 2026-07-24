package svaga.tgbottest.controller;

import lombok.val;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import svaga.tgbottest.DTO.BroadcastResult;
import svaga.tgbottest.DTO.OrderView;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.repository.DoctorRepository;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.service.*;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/tg/admin")
public class AdminController {

    private final OrderService orderService;
    private final BroadcastService broadcastService;
    private final OrderRepository orderRepository;
    private final DoctorRepository doctorRepository;
    private final ToothService toothService;
    private final DoctorService doctorService;
    private final FileStorageService fileStorageService;

    public AdminController(OrderService orderService, BroadcastService broadcastService, OrderRepository orderRepository, DoctorRepository doctorRepository, ToothService toothService, DoctorService doctorService, FileStorageService fileStorageService) {
        this.orderService = orderService;
        this.broadcastService = broadcastService;
        this.orderRepository = orderRepository;
        this.doctorRepository = doctorRepository;
        this.toothService = toothService;
        this.doctorService = doctorService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/orders")
    public String showPendingOrders(Model model) {
        List<Order> orders = orderService.getAllVariablePendingOrders();
        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(order, toothService.getActiveBalance(order.getUser())))
                .toList();

        model.addAttribute("orders", orderViews);
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "orders";
    }

    @GetMapping("/doctors")
    public String showDoctors(Model model) {
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "doctors";
    }

    @PostMapping("/doctors")
    public String createDoctor(
            @RequestParam String fullName,
            @RequestParam MultipartFile photo,
            @RequestParam(required = false) MultipartFile video,
            RedirectAttributes redirectAttributes) {

        doctorService.createDoctor(fullName, photo, video, redirectAttributes);

        return "redirect:/tg/admin/doctors";
    }

    @PostMapping("/doctors/{id}/delete")
    public String deleteDoctor(@PathVariable Long id) {
        orderRepository.deleteAllByDoctorId(id);   // сначала каскадом сносим заявки
        doctorRepository.deleteById(id);           // потом самого врача
        return "redirect:/tg/admin/doctors";
    }

    @PostMapping("/doctors/{id}/update")
    public String updateDoctor(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam(required = false) MultipartFile video,
            RedirectAttributes redirectAttributes) {

        try {
            doctorService.updateDoctor(id, fullName, photo, video, redirectAttributes);
            redirectAttributes.addFlashAttribute("success", "Врач обновлён!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления");
        }
        return "redirect:/tg/admin/doctors";
    }

/*    @GetMapping("/orders/fragment")
    public String ordersFragment(Model model) {
        List<Order> orders = orderService.getPendingOrders();

        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(order, toothService.getActiveBalance(order.getUser())))
                .toList();

        model.addAttribute("orders", orderViews);
        model.addAttribute("doctors", doctorRepository.findAllByOrderByFullNameAsc());
        return "orders :: ordersListFragment";
    }*/

    @PostMapping("/order/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/tg/admin/orders";
    }

    @PostMapping("/order/{id}/confirm")
    public String confirmOrder(
            @PathVariable Long id,
            @RequestParam Long doctorId,
            @RequestParam("appointmentDate") String dateTimeStr)
            throws ParseException {
        LocalDateTime appointmentDate = LocalDateTime.parse(dateTimeStr);
        orderService.confirmOrder(id, doctorId, appointmentDate);
        return "redirect:/tg/admin/orders";
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
        String redirect = "redirect:/tg/admin/completed";
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
        String redirect = "redirect:/tg/admin/completed";
        if (phone != null && !phone.isBlank()) redirect += "?phone=" + phone;
        if (date != null) redirect += (redirect.contains("?") ? "&" : "?") + "date=" + date;
        return redirect;
    }

    @GetMapping("/broadcast")
    public String showBroadcastPage(Model model) {
        return "broadcast";
    }

    @PostMapping("/broadcast/send")
    public String sendBroadcast(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "all") String mode,
            @RequestParam(required = false) String phoneList,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate broadcastDate,
            Model model) {

        try {
            BroadcastResult result = broadcastService.sendBroadcast(message, mode, phoneList, broadcastDate);

            model.addAttribute("success",
                    "Рассылка завершена! Отправлено: " + result.sent +
                            (result.failed > 0 ? " | Не удалось: " + result.failed : "") +
                            " (всего: " + result.total + ")");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Произошла ошибка при отправке: " + e.getMessage());
        }

        return "broadcast";
    }

    @GetMapping("/test-proxy")
    @ResponseBody
    public String testProxy() {
        StringBuilder sb = new StringBuilder("=== Тест SOCKS5 Прокси ===\n\n");

        try {
            // Для Docker + host.docker.internal
            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress("172.18.0.1", 1080));

            sb.append("1. Прокси создан (host.docker.internal:1080)\n");

            URL url = new URL("https://api.telegram.org"); // IP api.telegram.org
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("HEAD");

            int responseCode = conn.getResponseCode();

            sb.append("2. Соединение успешно!\n");
            sb.append("Код ответа: ").append(responseCode).append("\n");
            sb.append("Время: ").append(new java.util.Date());

        } catch (Exception e) {
            sb.append("❌ Ошибка: ")
                    .append(e.getClass().getSimpleName())
                    .append("\nСообщение: ")
                    .append(e.getMessage());
        }

        return sb.toString();
    }
}
