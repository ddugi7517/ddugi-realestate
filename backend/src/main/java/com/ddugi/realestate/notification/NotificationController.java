package com.ddugi.realestate.notification;

import com.ddugi.realestate.notification.service.RealEstateNotificationService;
import com.ddugi.realestate.notification.service.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Tag(name = "Notification", description = "텔레그램 알림 API")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final RealEstateNotificationService notificationService;
    private final TelegramBotService            telegramBotService;

    @Operation(summary = "봇 연결 테스트", description = "텔레그램 봇 설정 확인 및 테스트 메시지 발송")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        boolean configured = telegramBotService.isConfigured();
        if (configured) {
            telegramBotService.sendMessage("✅ 뚜기부동산 봇 연결 성공\\!");
        }
        return ResponseEntity.ok(Map.of(
            "configured", configured,
            "message", configured ? "테스트 메시지 발송 완료" : "TELEGRAM_BOT_TOKEN 또는 TELEGRAM_CHAT_ID 미설정"
        ));
    }

    @Operation(summary = "일일 종합 리포트 수동 발송")
    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> sendReport(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolve(yearMonth);
        notificationService.sendDailyReport(target);
        return ResponseEntity.ok(Map.of("status", "sent", "yearMonth", target));
    }

    @Operation(summary = "추천 매물 알림 수동 발송")
    @PostMapping("/recommended")
    public ResponseEntity<Map<String, Object>> sendRecommended(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolve(yearMonth);
        notificationService.sendRecommendedAlert(target);
        return ResponseEntity.ok(Map.of("status", "sent", "yearMonth", target));
    }

    @Operation(summary = "급등 매물 알림 수동 발송")
    @PostMapping("/rising")
    public ResponseEntity<Map<String, Object>> sendRising(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolve(yearMonth);
        notificationService.sendRisingAlert(target);
        return ResponseEntity.ok(Map.of("status", "sent", "yearMonth", target));
    }

    @Operation(summary = "급락 매물 알림 수동 발송")
    @PostMapping("/falling")
    public ResponseEntity<Map<String, Object>> sendFalling(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolve(yearMonth);
        notificationService.sendFallingAlert(target);
        return ResponseEntity.ok(Map.of("status", "sent", "yearMonth", target));
    }

    @Operation(summary = "전체 알림 일괄 수동 발송")
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> sendAll(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolve(yearMonth);
        notificationService.sendAll(target);
        return ResponseEntity.ok(Map.of("status", "sent", "yearMonth", target));
    }

    private String resolve(String yearMonth) {
        return yearMonth != null ? yearMonth
            : LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
