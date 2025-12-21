package svaga.tgbottest.repository;

import org.springframework.stereotype.Repository;
import svaga.tgbottest.model.ToothTransaction;
import svaga.tgbottest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToothTransactionRepository extends JpaRepository<ToothTransaction, Long> {

    List<ToothTransaction> findByUserOrderByCreatedAtAsc(User user);

    // Сумма всех зубиков, начисленных после определённой даты
    @Query("SELECT COALESCE(SUM(tt.amount), 0) FROM ToothTransaction tt " +
            "WHERE tt.user = :user AND tt.createdAt > :date")
    int sumAmountAfterDate(@Param("user") User user, @Param("date") LocalDateTime date);
}