package com.example.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private Long accessTokenExpirySeconds;
        private Long refreshTokenExpirySeconds;
        private String publicKey;
        private String privateKey;
    }

    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl;
    }
}

