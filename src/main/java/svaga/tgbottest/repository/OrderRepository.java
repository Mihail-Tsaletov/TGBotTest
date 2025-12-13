package svaga.tgbottest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import svaga.tgbottest.model.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(String status);
}
