package svaga.tgbottest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import svaga.tgbottest.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
    @Query("SELECT DISTINCT o.user FROM Order o WHERE DATE(o.appointmentDate) = :date AND o.status IN ('confirmed')")
    List<User> findDistinctUsersByAppointmentDate(@Param("date") LocalDate date);
    List<User> findByPhoneIn(List<String> phones);
}
