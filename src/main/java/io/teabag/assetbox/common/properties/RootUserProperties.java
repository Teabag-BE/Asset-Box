package io.teabag.assetbox.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "custom.rootuser")
public class RootUserProperties {
    private String email;
    private String password;
    private String username;
}

