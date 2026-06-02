package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.repository.UserReposiotry;
import io.teabag.assetbox.util.UserUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("UserService의")
class UserServiceTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserReposiotry userReposiotry;
    @Autowired
    UserService userService;


    @Nested
    @DisplayName("Describe: signup() 메서드에서")
    class Describe_with_signup{

        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456";


        @BeforeEach
        void setUp(){
           testUser =  UserUtil.createUser(
                   USER_EMAIL,
                   USER_PASSWORD
           );
           userReposiotry.save(testUser);
        }

        @Nested
        @DisplayName("Context: 올바른 데이터가 주어지는 경우")
        class Context_with_available_data{

            @Test
            @DisplayName("It: 유저가 성공적으로 생성")
            void It_유저_성공적으로_생성(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "wjd747"
                );
                // when
                UserCreateResponse savedUserResponse = userService.signup(request);
                // then
                Assertions.assertThat(savedUserResponse).isNotNull();
                Assertions.assertThat(savedUserResponse.email()).isEqualTo("testuser1@naver.com");
                Assertions.assertThat(savedUserResponse.major()).isEqualTo(Major.BACK_END.toString());

            }
        }

        @Nested
        @DisplayName("Context: 중복된 이메일 계정으로 가입 시도 시")
        class Context_with_duplicated_data{

            @Test
            @DisplayName("It: 유저 생성 실패됨")
            void It_유저_생성_실패(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        USER_EMAIL,
                        USER_PASSWORD
                );
                Assertions.assertThatThrownBy(
                        // when
                        ()-> userService.signup(request)
                )
                // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getDescription());

            }
        }
    }
}