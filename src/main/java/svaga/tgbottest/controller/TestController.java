/*
package svaga.tgbottest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import svaga.tgbottest.service.TelegramService;

@Controller
public class TestController {
    private final static Logger log = LoggerFactory.getLogger(TestController.class);

    private final TelegramService telegramService;

    public TestController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("status", "");
        return "index";
    }

    @PostMapping("/send")
    public String send(Model model) {
        boolean success = telegramService.sendConfirmation();
        model.addAttribute("status",
                success ? "Сообщение успешно отправлено!" : "Ошибка отправки в Telegram");
        return "index";
    }
}*/
