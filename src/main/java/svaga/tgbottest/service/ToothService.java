package svaga.tgbottest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import svaga.tgbottest.model.User;
import svaga.tgbottest.model.ToothTransaction;
import svaga.tgbottest.repository.ToothTransactionRepository;

import java.time.LocalDateTime;

@Service
public class ToothService {

    private final ToothTransactionRepository toothTransactionRepository;

    public ToothService(ToothTransactionRepository toothTransactionRepository) {
        this.toothTransactionRepository = toothTransactionRepository;
    }

    /**
     * Возвращает текущий активный баланс зубиков (не сгоревшие за год)
     */
    @Transactional(readOnly = true)
    public int getActiveBalance(User user) {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        return toothTransactionRepository.sumAmountAfterDate(user, oneYearAgo);
    }

    /**
     * Начисление зубиков
     */
    @Transactional
    public void earnTooth(User user, int amount, String reason) {
        if (amount <= 0) return;

        ToothTransaction tt = new ToothTransaction();
        tt.setUser(user);
        tt.setAmount(amount);
        tt.setReason(reason);
        toothTransactionRepository.save(tt);
    }

    /**
     * Списание зубиков
     */
    @Transactional
    public void spendTooth(User user, int amount, String reason) {
        if (amount <= 0) return;

        int balance = getActiveBalance(user);
        if (amount > balance) {
            throw new IllegalArgumentException("Недостаточно зубиков. Доступно: " + balance);
        }

        ToothTransaction tt = new ToothTransaction();
        tt.setUser(user);
        tt.setAmount(-amount);
        tt.setReason(reason);
        toothTransactionRepository.save(tt);
    }
}