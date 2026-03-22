package com.ddugi.realestate.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private Bot bot = new Bot();
    private Notification notification = new Notification();

    @Getter
    @Setter
    public static class Bot {
        private String token;
        private String chatId;
    }

    @Getter
    @Setter
    public static class Notification {
        private int topCount = 5;
        private double risingThreshold = 3.0;
        private double fallingThreshold = -3.0;
    }
}
