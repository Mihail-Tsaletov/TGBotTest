package svaga.tgbottest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import svaga.tgbottest.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
}
