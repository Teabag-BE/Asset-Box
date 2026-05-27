package io.teabag.assetbox.common.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // TODO: create initial admin user after User signup/login policy is implemented.
    }
}
