package svaga.tgbottest.DTO;

import lombok.Data;
import svaga.tgbottest.model.Doctor;
import svaga.tgbottest.model.Order;
import svaga.tgbottest.model.User;

import java.time.LocalDateTime;

@Data
public class OrderView {

    private final Long id;
    private final User user;
    private final Doctor doctor;
    private final LocalDateTime appointmentDate;
    private final String status;
    private final LocalDateTime createdAt;
    private final int activeToothBalance;  // рассчитанный баланс зубиков

    public OrderView(Order order, int activeToothBalance) {
        this.id = order.getId();
        this.user = order.getUser();
        this.doctor = order.getDoctor();
        this.appointmentDate = order.getAppointmentDate();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.activeToothBalance = activeToothBalance;
    }

    public String getDoctorName() {
        return doctor != null ? doctor.getFullName() : "Неизвестный врач";
    }
}