package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.constants.TokenType;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostLike;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.util.PostUtil;
import io.teabag.assetbox.util.RequestPostUtil;
import io.teabag.assetbox.util.UserUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService의")
class UserServiceTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserEmailRepository userReposiotry;
    @Autowired
    PostRepository postRepository;
    @Autowired
    UserService userService;
    @Autowired
    TokenProvider tokenProvider;
    @Autowired
    UserRepository realUserRepository;


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
                   passwordEncoder.encode(USER_PASSWORD)
           );
           userReposiotry.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 올바른 데이터가 주어지는 경우")
        class Context_with_available_data{

            @BeforeEach
            void setUp(){
                // 화이트리스트 추가
                userReposiotry.emailWhiteListSave(
                        EmailWhiteList.builder()
                                .email("testuser1@naver.com")
                                .build()
                );
            }

            @Test
            @DisplayName("It: 유저가 성공적으로 생성")
            void It_유저_성공적으로_생성(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "wjdtn0619",
                        "일정수"
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
        @DisplayName("Context: 올바르지 않은 데이터가 주어지는 경우")
        class Context_with_invalid_data{

            @BeforeEach
            void setUp(){
                // 화이트리스트 추가
                userReposiotry.emailWhiteListSave(
                        EmailWhiteList.builder()
                                .email(USER_EMAIL)
                                .build()
                );
            }

            @Test
            @DisplayName("It: 중복된 이메일 계정으로 가입 시도 시 유저 생성 실패됨")
            void It_유저_중복데이터_생성_실패(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        USER_EMAIL,
                        USER_PASSWORD,
                        "일정수"
                );
                Assertions.assertThatThrownBy(
                        // when
                        ()-> userService.signup(request)
                )
                // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_EMAIL_DUPLICATED.getDescription());

            }



            @Test
            @DisplayName("It: 화이트리스트에 없는 이메일 계정으로 가입 시도 시 유저 생성 실패됨")
            void It_유저_화이트리스트_없음_생성_실패(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        USER_PASSWORD,
                        "일정수"
                );
                Assertions.assertThatThrownBy(
                                // when
                                ()-> userService.signup(request)
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_EMAIL_NOT_WHITELISTED.getDescription());

            }
        }
    }


    @Nested
    @DisplayName("Describe: signIn() 메서드에서")
    class Describe_with_signin{

        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456";


        @BeforeEach
        void setUp(){
            testUser =  UserUtil.createUser(
                    USER_EMAIL,
                    passwordEncoder.encode(USER_PASSWORD)
            );
            userReposiotry.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 올바른 이메일과 비밀번호가 주어지는 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It: Access Token과 Refresh Token을 발급 후 반환")
            void It_토큰_생성_및_반환(){
                // given
                LoginRequest request = new LoginRequest(USER_EMAIL, USER_PASSWORD);

                // when
                KeyPair issuedKeyPair = userService.signIn(request);


                String accessToken = issuedKeyPair.accessToken();
                String refreshToken = issuedKeyPair.refreshToken();

                // then
                Assertions.assertThat(accessToken).isNotNull();
                Assertions.assertThat(refreshToken).isNotNull();

                TokenBody accessTokenBody = tokenProvider.parseJwt(accessToken);
                TokenBody refreshTokenBody = tokenProvider.parseJwt(refreshToken);

                Assertions.assertThat(accessTokenBody.email()).isEqualTo(request.email());
                Assertions.assertThat(refreshTokenBody.email()).isEqualTo(request.email());

            }

        }

        @Nested
        @DisplayName("Context: 잘못된 데이터가 주어지는 경우")
        class Context_with_invalid_data{

            @Test
            @DisplayName("It: 잘못된 비밀번호가 주어지는 경우")
            void It_잘못된_비밀번호로_로그인_실패(){
                // given
                LoginRequest request = new LoginRequest(USER_EMAIL, "잘못됨거임ㅋㅋㅋㅋ");

                // when
                Assertions.assertThatThrownBy(
                        ()->userService.signIn(request)
                )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.LOGIN_FAILED.getDescription());
            }

            @Test
            @DisplayName("It: 탈퇴한 계정에 로그인을 수행하는 경우")
            void It_이미_탈퇴한_경우_로그인_실패(){
                // given
                testUser.setDeletedAt();
                LoginRequest request = new LoginRequest(USER_EMAIL, USER_PASSWORD);

                // when
                Assertions.assertThatThrownBy(
                                ()->userService.signIn(request)
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_ALREADY_DELETED.getDescription());
            }

        }

    }

    @Nested
    @DisplayName("Describe : getUserDetailsByAdmin() 메서드에서")
    class Describe_with_getUserDetailsByAdmin{

        CurrentUser testUserDetails;

        User testUser;

        @BeforeEach
        void setUp(){
            realUserRepository.deleteAll();

            testUserDetails = CurrentUser.from(
                    UserUtil.createUser(
                            "testUser0@naver.com",
                            passwordEncoder.encode("wjdtn747")
                    )
            );

            int ITER = 20;

            for(int i = 0 ; i < ITER ; i++){
                User tester = realUserRepository.save(
                        UserUtil.createUser(
                                "testUser%d@naver.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "정수"
                        )
                );
                tester.updateRole(Role.USER);
            }
            for(int i = ITER ; i < 2 * ITER ; i++){
                User tester = realUserRepository.save(
                        UserUtil.createUser(
                                "testUser%d@google.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "유리수"
                        )
                );
                tester.updateRole(Role.ADMIN);
            }
            for(int i = 2 * ITER ; i < 3 * ITER ; i++){
                User tester = realUserRepository.save(
                        UserUtil.createUser(
                                "testUser%d@kakao.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "실수"
                        )
                );
                tester.updateRole(Role.SUPER_ADMIN);
            }
            User founded = realUserRepository.findByEmailOrThrow("testUser0@naver.com");
            founded.updateRole(Role.ADMIN);
        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어지는 경우")
        class Context_with_valid_data{
            @Test
            @DisplayName("It : 어드민에 의해 성공적으로 조회 후 반환")
            void It_성공적으로_조회(){

                SecurityContextHolder.getContext().setAuthentication(
                        new TestingAuthenticationToken(
                                testUserDetails,
                                null,
                                "ROLE_ADMIN"
                        )
                );

                AdminsUserDetailResponse founded = userService.getUserDetailsByAdmin(
                        testUserDetails.getEmail(),
                        PageRequest.of(0, 100),
                        null,
                        null
                );

                Assertions.assertThat(founded.items().size()).isEqualTo(60);
            }
        }

        @Nested
        @DisplayName("Context : 인증이 없는 경우")
        class Context_with_invalid_data{
            @Test
            @DisplayName("It : 권한이 없음")
            void It_권한이_없으므로_조회_실패(){

                // given
                SecurityContextHolder.getContext().setAuthentication(
                        new TestingAuthenticationToken(
                                testUserDetails,
                                null,
                                "ROLE_ADMIN"
                        )
                );

                // when
                Assertions.assertThatThrownBy(
                        ()->{
                            userService.getUserDetailsByAdmin(
                                    "wrong@gmail.com",
                                    PageRequest.of(0, 100),
                                    null,
                                    null
                            );
                        }
                )
                        // then
                 .isInstanceOf(AuthorizationDeniedException.class);
            }
        }

    }


}