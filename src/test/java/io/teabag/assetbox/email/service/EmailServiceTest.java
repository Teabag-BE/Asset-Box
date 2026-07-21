package io.teabag.assetbox.email.service;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.email.constants.EmailStatus;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import io.teabag.assetbox.email.dto.EmailWhiteListSearch;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.user.service.UserService;
import io.teabag.assetbox.util.UserUtil;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmailServiceTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserEmailRepository userEmailRepository;
    @Autowired
    EmailService emailService;

    User testAdmin;
    String USER_EMAIL = "testuser1@naver.com";
    String USER_PASSWORD = "123456789";
    TestingAuthenticationToken token;
    
    
    @BeforeEach
    void setUp(){
        testAdmin = UserUtil.createUser(
                USER_EMAIL,
                passwordEncoder.encode(USER_PASSWORD)
        );
        testAdmin.updateRole(Role.ADMIN);
        userEmailRepository.userSave(testAdmin);

        token = new TestingAuthenticationToken(
                CurrentUser.from(testAdmin),
                null,
                "ROLE_ADMIN"
        );
    }

    @Nested
    @DisplayName("Describe : enrollEmail()의 이메일 화이트 리스트 등록에서")
    class Describe_enrollEmail {

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_available_data{

            @Test
            @DisplayName("It : 이메일을 화이트리스트 등록")
            void It_이메일을_화이트리스트에_등록(){
                // given
                SecurityContextHolder.getContext().setAuthentication(token);

                // when
                emailService.enrollEmail(
                        testAdmin.getEmail(),
                        new EnrollEmailRequest(
                        "whitelist@naver.com",
                        "화이트리스트이용자",
                        Major.BACK_END.toString()
                )
                );

                // then
                EmailWhiteList founded = userEmailRepository.findEmailWhiteListByEmailOrThrow("whitelist@naver.com");
                Assertions.assertThat(founded.getMajor()).isEqualTo(Major.BACK_END);
                Assertions.assertThat(founded.getEmailStatus()).isEqualTo(EmailStatus.ENROLL);
            }
        }

        @Nested
        @DisplayName("Context : 적합한 권한이 없거나 잘못된 요청인 경우")
        class Context_with_Non_Valid_Authority{

            @Test
            @DisplayName("It : 적합한 권한이 없는 경우 이메일을 화이트리스트로 등록하지 못한다.")
            void It_이메일_화이트리스트_등록_실패(){
                // given
                User testUser = userEmailRepository.userSave(UserUtil.createUser(
                        "wjdtn747@na.com",
                        passwordEncoder.encode("wjdtn1231312")
                ));

                TestingAuthenticationToken testingToken = new TestingAuthenticationToken(
                        CurrentUser.from(testUser),
                        null,
                        "ROLE_USER"
                );
                SecurityContextHolder.getContext().setAuthentication(testingToken);

                // when
                Assertions.assertThatThrownBy(
                        () -> emailService.enrollEmail(
                                testUser.getEmail(),
                                new EnrollEmailRequest(
                                        "whitelist@naver.com",
                                        "화이트리스트이용자",
                                        Major.BACK_END.toString()
                                )
                        )
                )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.ACCOUNT_NOT_ADMIN.getDescription());
            }

            @Test
            @DisplayName("It : 이미 등록된 이메일을 화이트리스트로 등록하지 못한다.")
            void It_중복_이메일_화이트리스트_등록_실패(){
                // given
                SecurityContextHolder.getContext().setAuthentication(token);

                emailService.enrollEmail(
                        testAdmin.getEmail(),
                        new EnrollEmailRequest(
                                "whitelist@naver.com",
                                "화이트리스트이용자",
                                Major.BACK_END.toString()
                        )
                );

                // when
                Assertions.assertThatThrownBy(
                                () -> emailService.enrollEmail(
                                        testAdmin.getEmail(),
                                        new EnrollEmailRequest(
                                                "whitelist@naver.com",
                                                "화이트리스트이용자",
                                                Major.BACK_END.toString()
                                        )
                                )
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.EMAIL_ALREADY_ON_WHITELIST.getDescription());
            }
        }

    }


    @Nested
    @DisplayName("Describe : getSearches()의 이메일 화이트 리스트 검색에서")
    class Describe_getSearches{

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_available_data{

            @BeforeEach
            void setUp(){
                SecurityContextHolder.getContext().setAuthentication(token);
                for(int i = 0 ; i < 20 ; i++){
                    emailService.enrollEmail(
                            USER_EMAIL,
                            new EnrollEmailRequest(
                                    "whitelist@naver.com" + i,
                                    "화이트리스트이용자" + i,
                                    Major.BACK_END.toString()
                            )
                    );
                }
            }

            @Test
            @DisplayName("It : 5개의 이메일을 정상적으로 검색")
            void It_이메일을_화이트리스트에_등록(){
                // given
                SecurityContextHolder.getContext().setAuthentication(token);

                // when
                Page<EmailWhiteListSearch> searches = emailService.getSearches(
                        USER_EMAIL,
                        PageRequest.of(0, 5)
                );


                // then
                Assertions.assertThat(searches.get().count()).isEqualTo(5);
            }
        }

        @Nested
        @DisplayName("Context : 적합한 권한이 없는 경우")
        class Context_with_Non_Valid_Authority{

            @Test
            @DisplayName("It : 적합한 권한이 없는 경우 화이트리스트 상 이메일을 껌색하지 못한다.")
            void It_이메일_화이트리스트_등록_실패(){
                // given
                User testUser = userEmailRepository.userSave(UserUtil.createUser(
                        "wjdtn747@na.com",
                        passwordEncoder.encode("wjdtn1231312")
                ));

                TestingAuthenticationToken testingToken = new TestingAuthenticationToken(
                        CurrentUser.from(testUser),
                        null,
                        "ROLE_USER"
                );
                SecurityContextHolder.getContext().setAuthentication(testingToken);

                // when
                Assertions.assertThatThrownBy(
                                () -> emailService.getSearches(
                                        testUser.getEmail(),
                                        PageRequest.of(0, 5)
                                )
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.ACCOUNT_NOT_ADMIN.getDescription());
            }
        }
    }

    @Nested
    @DisplayName("Describe : deleteEmailFromWhiteList()의 이메일 화이트 리스트 삭제에서")
    class Describe_deleteEmailFromWhiteList{

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_available_data{

            @Test
            @DisplayName("It : 화이트리스트 상 이메일을 정상적으로 삭제")
            void It_이메일을_화이트리스트에_등록(){
                // given
                SecurityContextHolder.getContext().setAuthentication(token);

                emailService.enrollEmail(
                        USER_EMAIL,
                        new EnrollEmailRequest(
                                "whitelist@naver.com",
                                "화이트리스트이용자",
                                Major.BACK_END.toString()
                        )
                );

                // when
                emailService.deleteEmailFromWhiteList(
                        USER_EMAIL,
                        "whitelist@naver.com"
                );


                // then
                Assertions.assertThatThrownBy(
                        ()->userEmailRepository.findEmailWhiteListByEmailOrThrow("whitelist@naver.com")
                ).isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.EMAIL_NOT_ON_WHITELIST.getDescription());
            }
        }

        @Nested
        @DisplayName("Context : 적합한 권한이 없는 경우")
        class Context_with_Non_Valid_Authority{

            @Test
            @DisplayName("It : 적합한 권한이 없는 경우 화이트리스트 상 이메일을 삭제하지 못한다.")
            void It_이메일_화이트리스트_등록_실패(){
                // given
                User testUser = userEmailRepository.userSave(UserUtil.createUser(
                        "wjdtn747@na.com",
                        passwordEncoder.encode("wjdtn1231312")
                ));
                TestingAuthenticationToken testingToken = new TestingAuthenticationToken(
                        CurrentUser.from(testUser),
                        null,
                        "ROLE_USER"
                );
                SecurityContextHolder.getContext().setAuthentication(testingToken);

                // when
                Assertions.assertThatThrownBy(
                                () -> emailService.deleteEmailFromWhiteList(
                                        testUser.getEmail(),
                                        "whitelist@naver.com"
                                )
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.ACCOUNT_NOT_ADMIN.getDescription());
            }
        }
    }
}