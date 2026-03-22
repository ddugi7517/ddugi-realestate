package com.ddugi.realestate.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ScraperConfig {

    @Bean
    public WebClient molitWebClient() {
        return WebClient.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "public-api.molit")
    public MolitApiProperties molitApiProperties() {
        return new MolitApiProperties();
    }

    @Getter
    @Setter
    public static class MolitApiProperties {
        private String serviceKey;
        private String aptTradeUrl;
        private String aptRentUrl;
    }
}
