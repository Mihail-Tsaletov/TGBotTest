package svaga.tgbottest.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class TelegramService {
    private final static Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final TelegramBot bot;
    private final Long chatId;
    @Autowired
    private FileStorageService fileStorageService;

    public TelegramService(@Value("${bot-token}") String botToken,
                           @Value("${chat-id}") Long chatId) {
        this.bot = new TelegramBot(botToken);
        this.chatId = chatId;
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId, text);
        SendResponse response = bot.execute(request);
        if (!response.isOk()) {
            log.error("Ошибка отправки в TG: {}", response.description());
        } else {
            log.info("Отправка сообщения успешна: {}, ответ {}", text, response.description());
        }
    }

    public void sendPhoto(Long chatId, String fileId, String caption) {
        SendPhoto sendPhoto = new SendPhoto(chatId.toString(), fileId);
        sendPhoto.caption(caption);
        sendPhoto.parseMode(ParseMode.HTML);
        bot.execute(sendPhoto);
    }

    public void sendVideo(Long chatId, String videoUrl, String caption) {
        // videoUrl — относительный путь
        Path videoPath = fileStorageService.getFilePath(videoUrl);
        // pengrad поддерживает SendVideo с file или URL
        SendVideo sendVideo = new SendVideo(chatId.toString(), videoPath.toFile());
        sendVideo.caption(caption);
        bot.execute(sendVideo);
    }
}