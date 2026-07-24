package svaga.tgbottest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import svaga.tgbottest.model.Order;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(String status);
    List<Order> findByStatusInOrderByCreatedAtAsc(List<String> statuses);

    @Query("SELECT o FROM Order o " +
            "WHERE o.status = 'confirmed' " +
            "AND DATE(o.appointmentDate) = :date " +
            "ORDER BY o.appointmentDate")
    List<Order> findConfirmedByAppointmentDate(@Param("date") LocalDate date);

    // 2. Поиск по дате + телефону (статус confirmed)
    @Query("SELECT o FROM Order o JOIN o.user u " +
            "WHERE o.status = 'confirmed' " +
            "AND DATE(o.appointmentDate) = :date " +
            "AND u.phone LIKE :phonePattern " +
            "ORDER BY o.appointmentDate")
    List<Order> findConfirmedByAppointmentDateAndPhone(
            @Param("date") LocalDate date,
            @Param("phonePattern") String phonePattern);


    void deleteAllByDoctorId(Long id);
}
