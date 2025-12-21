package svaga.tgbottest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    private LocalDateTime appointmentDate;
    private BigDecimal price;
    private String status = "pending";
    private LocalDateTime confirmedAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
