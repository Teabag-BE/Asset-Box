package io.teabag.assetbox.common.config;



import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.EmailWhiteListRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DataInitConfig {
    private final UserRepository userRepository;
    private final EmailWhiteListRepository emailWhiteListRepository;
    private final PasswordEncoder passwordEncoder;
    @Bean
    @Transactional
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() < 1) { // 중복 방지
                User build1 = User.builder()
                        .name("이정수")
                        .nickname("정수리")
                        .major(Major.BACK_END)
                        .email("wjdtn747@naver.com")
                        .password(passwordEncoder.encode("wjdtn3902"))
                        .build();
                build1.updateRole(Role.SUPER_ADMIN);
                userRepository.save(build1);
                emailWhiteListRepository.save(
                        EmailWhiteList.builder()
                                .email("wjdtn747@gmail.com")
                                .build()
                );
            }
        };
    }
}
