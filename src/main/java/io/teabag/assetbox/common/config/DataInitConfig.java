package io.teabag.assetbox.common.config;



import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserReposiotry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DataInitConfig {
    private final UserReposiotry userReposiotry;
    private final PasswordEncoder passwordEncoder;
    @Bean
    @Transactional
    CommandLineRunner init(UserReposiotry userReposiotry) {
        return args -> {
            if (userReposiotry.count() < 1) { // 중복 방지
                User build1 = User.builder()
                        .name("이정수")
                        .nickname("정수리")
                        .major(Major.BACK_END)
                        .email("wjdtn747@naver.com")
                        .password(passwordEncoder.encode("1234"))
                        .build();
                build1.setSuperAdmin();
                userReposiotry.save(build1);
                User build = User.builder()
                        .name("노동훈")
                        .nickname("노---동훈")
                        .major(Major.BACK_END)
                        .email("wjdtn747@gmail.com")
                        .password(passwordEncoder.encode("1234"))
                        .build();
                build.setSuperAdmin();
                userReposiotry.save(build);
            }
        };
    }
}
