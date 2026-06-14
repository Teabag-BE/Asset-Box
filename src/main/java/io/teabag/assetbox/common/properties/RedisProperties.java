package io.teabag.assetbox.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "custom.redis")
public class RedisProperties {
    private String host;
    private String port;
}
