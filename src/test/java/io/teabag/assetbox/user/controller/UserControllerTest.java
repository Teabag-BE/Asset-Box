package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserEmailRepository userEmailRepository;
    @Autowired
    ObjectMapper objectMapper;

    String BASE_URL = "/api/users";

    @Nested
    @DisplayName("Description: 회원가입 ( POST /api/users/signup )")
    class Description_with_sign_up {

        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456789";

        @BeforeEach
        void setUp() {
            testUser = UserUtil.createUser(
                    USER_EMAIL,
                    USER_PASSWORD
            );
            userEmailRepository.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 올바른 데이터가 주어지는 경우")
        class Context_with_available_data {

            @BeforeEach
            void setUp(){
                // 화이트리스트 추가
                userEmailRepository.emailWhiteListSave(
                        EmailWhiteList.builder()
                                .email("testuser1@naver.com")
                                .build()
                );
            }

            @Test
            @DisplayName("It: 성공적으로 유저를 생성하여 201과 유저정보를 반환한다.")
            void It_유저_성공적으로_생성_및_201_반환() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "456784314131",
                        "일정수"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );
                // then
                perform
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value(SuccessCode.USER_CREATED.getSuccessMessage()))
                        .andExpect(jsonPath("$.data.email").value(request.email()))
                        .andExpect(jsonPath("$.data.name").value(request.name()));
            }
        }

        @Nested
        @DisplayName("Context: 잘못된 데이터로 회원가입을 수행하는 경우")
        class Context_with_invalid_data {

            @BeforeEach
            void setUp(){
                // 화이트리스트 추가
                userEmailRepository.emailWhiteListSave(
                        EmailWhiteList.builder()
                                .email("testuser1@naver.com")
                                .build()
                );
            }

            @Test
            @DisplayName("It: 이메일 형식 위반 위배 시 400 에러 발생")
            void It_유저_생성_실패_및_400_반환() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "wjdtn747",
                        USER_PASSWORD,
                        "일정수"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(
                                MockMvcResultMatchers
                                        .status().is4xxClientError()
                        )
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 패스워드 길이 위배 시 400 에러 발생")
            void It_유저_생성_실패_및_400_반환2() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "1234567",
                        "일정수"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(
                                MockMvcResultMatchers
                                        .status().is4xxClientError()
                        )
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 닉네임 길이 위배 시 400 에러 발생")
            void It_유저_생성_실패_및_400_반환3() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "123456755",
                        "일"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(
                                MockMvcResultMatchers
                                        .status().is4xxClientError()
                        )
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 화이트리스트 등록 이메일 아니면 403 에러 발생")
            void It_유저_생성_실패_및_403_반환() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "notWhiteList@naver.com",
                        USER_PASSWORD,
                        "일정수"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(
                                MockMvcResultMatchers
                                        .status().is4xxClientError()
                        )
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.USER_EMAIL_NOT_WHITELISTED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.USER_EMAIL_NOT_WHITELISTED.getDescription()));
            }

            @Test
            @DisplayName("It: 중복된 이메일 이면 409 에러 발생")
            void It_유저_생성_실패_및_409_반환() throws Exception {
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        USER_EMAIL,
                        USER_PASSWORD,
                        "일정수"
                );

                String json = objectMapper.writeValueAsString(request);
                // when
                ResultActions perform = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(
                                MockMvcResultMatchers
                                        .status().is4xxClientError()
                        )
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.USER_EMAIL_DUPLICATED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.USER_EMAIL_DUPLICATED.getDescription()));
            }


        }
    }
}