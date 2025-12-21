package svaga.tgbottest.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import svaga.tgbottest.DTO.BroadcastResult;
import svaga.tgbottest.model.User;
import svaga.tgbottest.repository.OrderRepository;
import svaga.tgbottest.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BroadcastService {

    private final UserRepository userRepository;
    private final TelegramService telegramService;

    public BroadcastService(UserRepository userRepository, TelegramService telegramService) {
        this.userRepository = userRepository;
        this.telegramService = telegramService;
    }

    @Transactional(readOnly = true)
    public BroadcastResult sendBroadcast(String message, String mode, String phoneList, LocalDate broadcastDate) {
        List<User> recipients = new ArrayList<>();

        switch (mode == null ? "all" : mode) {
            case "all":
                recipients = userRepository.findAll();
                break;

            case "by_date":
                if (broadcastDate == null) {
                    throw new IllegalArgumentException("Дата не указана для рассылки по записи");
                }
                recipients = userRepository.findDistinctUsersByAppointmentDate(broadcastDate);
                break;

            case "by_phone":
                if (phoneList == null || phoneList.trim().isEmpty()) {
                    throw new IllegalArgumentException("Список телефонов пуст");
                }
                // Разбиваем по запятым, пробелам, переносам строк
                List<String> phones = List.of(phoneList.split("[,;\\s\n]+"))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                recipients = userRepository.findByPhoneIn(phones);
                break;

            default:
                throw new IllegalArgumentException("Неизвестный режим рассылки");
        }

        int sentCount = 0;
        int failedCount = 0;

        for (User user : recipients) {
            try {
                telegramService.sendMessage(user.getChatId(), message);
                sentCount++;
            } catch (Exception e) {
                failedCount++;
                System.err.println("Ошибка отправки пользователю " + user.getPhone() + " (chat_id: " + user.getChatId() + "): " + e.getMessage());
            }
        }

        return new BroadcastResult(sentCount, failedCount, recipients.size());
    }
}
