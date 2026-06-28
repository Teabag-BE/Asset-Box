package io.teabag.assetbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AssetBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetBoxApplication.class, args);
    }

}
