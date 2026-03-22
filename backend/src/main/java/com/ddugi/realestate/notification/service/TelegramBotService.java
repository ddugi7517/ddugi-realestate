package com.ddugi.realestate.notification.service;

import com.ddugi.realestate.notification.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * 텔레그램 메시지 전송 서비스
 * DefaultAbsSender를 상속해 봇 토큰/옵션을 주입
 */
@Slf4j
@Service
public class TelegramBotService extends DefaultAbsSender {

    private final TelegramProperties telegramProperties;

    public TelegramBotService(TelegramProperties telegramProperties) {
        super(new DefaultBotOptions(), telegramProperties.getBot().getToken());
        this.telegramProperties = telegramProperties;
    }

    /**
     * 기본 채팅방으로 마크다운 메시지 전송
     */
    public void sendMessage(String text) {
        sendMessage(telegramProperties.getBot().getChatId(), text);
    }

    /**
     * 특정 채팅방으로 마크다운 메시지 전송
     */
    public void sendMessage(String chatId, String text) {
        if (!isConfigured()) {
            log.warn("[Telegram] 봇 설정 미완료 - 메시지 전송 스킵 (token 또는 chatId 없음)");
            return;
        }
        if (!StringUtils.hasText(text)) return;

        try {
            // 텔레그램 메시지 최대 길이 4096자
            List<String> chunks = splitMessage(text, 4096);
            for (String chunk : chunks) {
                SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(chunk)
                    .parseMode("MarkdownV2")
                    .disableWebPagePreview(true)
                    .build();
                execute(message);
            }
            log.info("[Telegram] 메시지 전송 완료 - chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 봇 설정 완료 여부 확인
     */
    public boolean isConfigured() {
        String token  = telegramProperties.getBot().getToken();
        String chatId = telegramProperties.getBot().getChatId();
        return StringUtils.hasText(token) && StringUtils.hasText(chatId);
    }

    /**
     * 긴 메시지를 maxLen 단위로 분할
     */
    private List<String> splitMessage(String text, int maxLen) {
        if (text.length() <= maxLen) return List.of(text);

        java.util.List<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
