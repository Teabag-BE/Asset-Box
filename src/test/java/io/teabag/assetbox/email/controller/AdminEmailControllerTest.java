package io.teabag.assetbox.email.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.email.dto.DeleteEmailRequest;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.email.service.EmailService;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.util.UserUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class AdminEmailControllerTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserEmailRepository userEmailRepository;
    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    EmailService emailService;

    String BASE_URL = "/api/admin/email";
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
    @DisplayName("Describe : POST /api/admin/email")
    class Describe_with_Enroll_Email{

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_Available_Data{

            @Test
            @DisplayName("It : 화이트리스트 상 이메일 등록 성공")
            void It_이메일_화이트리스트_등록__성공() throws Exception {

                // given
                SecurityContextHolder.getContext().setAuthentication(token);
                String json = objectMapper.writeValueAsString(
                        new EnrollEmailRequest(
                                "whitelist@naver.com",
                                "화이트리스트이용자",
                                Major.BACK_END.toString()
                        )
                );


                // when
                mockMvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                ).andDo(print())

                        // then
                        .andExpect(MockMvcResultMatchers.status().isCreated())
                        .andExpect(jsonPath("$.message").value(SuccessCode.MAIL_ENROLL_COMPLETE.getSuccessMessage()));

            }
        }

        @Nested
        @DisplayName("Context : 적합한 권한이 없거나 잘못된 요청인 경우")
        class Context_with_Non_Valid_Authority{

            @Test
            @DisplayName("It : 적합한 권한이 없는 경우 이메일을 화이트리스트로 등록하지 못한다.")
            void It_이메일_화이트리스트_등록_실패() throws Exception {
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

                String json = objectMapper.writeValueAsString(
                        new EnrollEmailRequest(
                                "whitelist@naver.com",
                                "화이트리스트이용자",
                                Major.BACK_END.toString()
                        )
                );

                // when
                mockMvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                ).andDo(print())
                        // then
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError());
            }

            @Test
            @DisplayName("It : 이미 등록된 이메일을 화이트리스트로 등록하지 못한다.")
            void It_중복_이메일_화이트리스트_등록_실패() throws Exception {
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

                String json = objectMapper.writeValueAsString(
                        new EnrollEmailRequest(
                                "whitelist@naver.com",
                                "화이트리스트이용자",
                                Major.BACK_END.toString()
                        )
                );

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.post(BASE_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        ).andDo(print())

                        // then
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.EMAIL_ALREADY_ON_WHITELIST.getDescription()));

            }
        }

    }

    @Nested
    @DisplayName("Describe : DELETE /api/admin/email")
    class Describe_with_Get_Email{

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_Available_Data{

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
            @DisplayName("It : 화이트리스트 상 이메일 검색 성공")
            void It_이메일_화이트리스트_검색__성공() throws Exception {

                // given
                SecurityContextHolder.getContext().setAuthentication(token);

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.get(BASE_URL)
                                        .param("page","0")
                                        .param("size","5")
                        ).andDo(print())

                        // then
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(jsonPath("$.message").value(SuccessCode.MAIL_WHITELIST_SEARCH_COMPLETE.getSuccessMessage()))
                        .andExpect(jsonPath("$.data.numberOfElements").value(5));

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
            void It_화이트리스트_상_이메일_삭제() throws Exception {
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

                String json = objectMapper.writeValueAsString(
                        new DeleteEmailRequest("whitelist@naver.com")
                );

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.delete(BASE_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        ).andDo(print())

                        // then
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(jsonPath("$.message").value(SuccessCode.MAIL_DELETE_COMPLETE.getSuccessMessage()));


                // then
                Assertions.assertThatThrownBy(
                                ()->userEmailRepository.findEmailWhiteListByEmailOrThrow("whitelist@naver.com")
                        ).isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.EMAIL_NOT_ON_WHITELIST.getDescription());
            }
        }
    }

}