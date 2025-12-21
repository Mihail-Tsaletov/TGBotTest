package svaga.tgbottest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tooth_transactions")
@Data
public class ToothTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount;  // положительное — начисление, отрицательное — списание

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String reason;  // например: "Начисление 1% за приём", "Списание на оплату"

    public ToothTransaction() {}

    public ToothTransaction(User user, Integer amount, String reason) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
    }
}