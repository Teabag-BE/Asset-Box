package io.teabag.assetbox.common.security.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.domain.RefreshToken;
import io.teabag.assetbox.common.security.repository.RefreshTokenRepository;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.RefreshResponse;
import io.teabag.assetbox.user.dto.TokenBody;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.util.UserUtil;
import jakarta.servlet.http.Cookie;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("OauthService의")
class OauthServiceTest {

    @Autowired
    OauthService oauthService;
    @Autowired
    TokenProvider tokenProvider;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    User testUser;
    String USER_EMAIL = "testuser2@naver.com";
    String USER_PASSWORD = "wjdtn74721231";


    @BeforeEach
    void setUp(){
        testUser = userRepository.save(
                UserUtil.createUser(USER_EMAIL, passwordEncoder.encode(USER_PASSWORD))
        );
    }

    @Nested
    @DisplayName("Describe: refreshToken() 메서드에서")
    class Describe_with_refresh{

        String refreshToken;

        @BeforeEach
        void setUp(){
            refreshToken = tokenProvider.issueKeyPair(
                    USER_EMAIL,
                    testUser.getRole()
            ).refreshToken();
        }

        @Nested
        @DisplayName("Context: 올바른 Refresh Token이 주어진 경우")
        class Context_with_valid_token{

            @Test
            @DisplayName("It: Access Token과 Refresh Token의 재발급에 성공한다.")
            void It_RT_재발급_성공(){
                // when
                KeyPair response = oauthService.refreshToken(refreshToken);

                // then
                String accessToken = response.accessToken();
                String refreshToken = response.refreshToken();
                Assertions.assertThat(accessToken).isNotNull();
                Assertions.assertThat(refreshToken).isNotNull();

                TokenBody extractedAccessToken = tokenProvider.parseJwt(accessToken);
                TokenBody extractedRefreshToken = tokenProvider.parseJwt(refreshToken);

                Assertions.assertThat(extractedAccessToken.email()).isEqualTo(USER_EMAIL);
                Assertions.assertThat(extractedRefreshToken.email()).isEqualTo(USER_EMAIL);

            }

        }

        @Nested
        @DisplayName("Context: 잘못된 Refresh Token이 주어진 경우")
        class Context_with_invalid_token{

            @Test
            @DisplayName("It: 만료된 RefreshToken인 경우 재발급에 실패한다.")
            void It_RT_재발급_실패(){
                // given
                refreshTokenRepository.delete(RefreshToken.builder().refreshToken(refreshToken).build());

                // when
                Assertions.assertThatThrownBy(
                        () -> oauthService.refreshToken(refreshToken)
                )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REFRESH_TOKEN_EXPIRED.getDescription());
            }


        }
    }

}