package io.teabag.assetbox.common.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {
    private final Payload payload;
    private final Secrets secrets;
    private final Validations validations;
    @Getter
    @RequiredArgsConstructor
    public static class Payload{
        private final String issuer;
        private final String subjectAccessToken;
        private final String subjectRefreshToken;
        private final String audiance;
    }
    @Getter
    @RequiredArgsConstructor
    public static class Secrets{
        private final String appKey;
        private final String vanillakey;
    }
    @Getter
    @RequiredArgsConstructor
    public static class Validations {
        private final Integer access;
        private final Integer refresh;
    }
}
