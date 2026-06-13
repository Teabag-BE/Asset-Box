package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.LoginRequest;
import io.teabag.assetbox.user.dto.RefreshResponse;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.TokenBody;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.util.UserUtil;
import jakarta.servlet.http.Cookie;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserEmailRepository userEmailRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TokenProvider tokenProvider;

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
                    passwordEncoder.encode(USER_PASSWORD)
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


    @Nested
    @DisplayName("Description: 로그인 ( POST /api/users/login )")
    class Descrition_with_sign_in{

        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456789";

        @BeforeEach
        void setUp() {
            testUser = UserUtil.createUser(
                    USER_EMAIL,
                    passwordEncoder.encode(USER_PASSWORD)
            );
            userEmailRepository.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 올바른 이메일과 비밀번호로 로그인을 수행하는 경우")
        class Describe_with_valid_data{

            @Test
            @DisplayName("It: 로그인 성공 및 Access / Refresh Token 발급")
            void It_로그인_성공_및_토큰_응답() throws Exception {
                // given
                LoginRequest request = new LoginRequest(USER_EMAIL, USER_PASSWORD);

                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(jsonPath("$.message").value(SuccessCode.USER_SIGNIN.getSuccessMessage()))
                        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
            }


        }

        @Nested
        @DisplayName("Context: 잘못된 데이터로 로그인을 수행하는 경우")
        class Describe_with_invalid_data {

            @ParameterizedTest
            @ValueSource(strings = { "", "wjdtn"})
            @DisplayName("It: 이메일 누락으로 인한 로그인 실패 및 400 에러 발생")
            void It_아이디_누락_로그인_실패_400_응답(String email) throws Exception {
                // given
                LoginRequest request = new LoginRequest(email, USER_PASSWORD);

                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
            }

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("It: 비밀번호 누락으로 인한 로그인 실패 및 400 에러 발생")
            void It_비밀번호_누락_로그인_실패_400_응답(String password) throws Exception {
                // given
                LoginRequest request = new LoginRequest(USER_EMAIL, password);


                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 잘못된 비밀번호로 인한 로그인 실패 및 401 에러 발생")
            void It_비밀번호_잘못됨_로그인_실패_401_응답() throws Exception {
                // given
                LoginRequest request = new LoginRequest(USER_EMAIL, "잘못됨ㅋㅋ");


                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.LOGIN_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 존재하지 않는 이메일로 인한 로그인 실패 및 401 에러 발생")
            void It_이메일_잘못됨_로그인_실패_401_응답() throws Exception {
                // given
                LoginRequest request = new LoginRequest("wjdtn@gmail.com", USER_PASSWORD);

                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_FAILED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.LOGIN_FAILED.getDescription()));
            }

            @Test
            @DisplayName("It: 이미 삭제된 계정으로의 로그인 실패 및 401 에러 발생")
            void It_로그인_실패_401_응답_이미_계정_삭제됨() throws Exception {
                // given
                testUser.setDeletedAt();
                LoginRequest request = new LoginRequest(USER_EMAIL, USER_PASSWORD);

                String json = objectMapper.writeValueAsString(request);

                // when
                ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                );

                // then
                actions
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(jsonPath("$.error.code").value(ErrorCode.USER_ALREADY_DELETED.toString()))
                        .andExpect(jsonPath("$.error.message").value(ErrorCode.USER_ALREADY_DELETED.getDescription()));
            }

        }
    }

    @Nested
    @DisplayName("Description: 토큰 재발급 ( POST /api/users/refresh )")
    class Description_with_Refresh_Token{


        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "wjdtn74721231";
        String refreshToken;
        String REFRESH_TOKEN_NAME = "RT";

        @BeforeEach
        void setUp(){
            testUser = userRepository.save(
                    UserUtil.createUser(USER_EMAIL, passwordEncoder.encode(USER_PASSWORD))
            );

            refreshToken = tokenProvider.issueKeyPair(testUser.getEmail(), testUser.getRole()).refreshToken();
        }

        @Nested
        @DisplayName("Context: 올바른 Refresh Token이 주어진 경우")
        class Context_with_valid_refrsh_token{

            @Test
            @DisplayName("It: Access Token과 Refresh Token의 재발급 성공 및 200 OK 반환")
            void It_토큰_재발급_성공() throws Exception {

                Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, refreshToken);

                MockHttpServletResponse response = mockMvc.perform(
                                MockMvcRequestBuilders
                                        .post(BASE_URL + "/refresh")
                                        .cookie(cookie)
                        ).andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(SuccessCode.TOKEN_REFRESH_COMPLETED.getSuccessMessage()))
                        .andReturn()
                        .getResponse();

                String json = response.getContentAsString();

                ApiResponse<RefreshResponse> responsedResult = objectMapper.readValue(
                        json,
                        new TypeReference<ApiResponse<RefreshResponse>>() {
                        }
                );

                TokenBody tokenBody = tokenProvider.parseJwt(responsedResult.data().accessToken());

                Assertions.assertThat(tokenBody.email()).isEqualTo(USER_EMAIL);


            }

        }
    }


}