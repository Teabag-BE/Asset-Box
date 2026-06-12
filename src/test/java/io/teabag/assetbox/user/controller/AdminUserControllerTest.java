package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.AdminsUserDetailResponse;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.user.service.UserService;
import io.teabag.assetbox.util.UserUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.test.autoconfigure.webmvc.SecurityMockMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.web.servlet.function.ServerResponse.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userReposiotry;

    String BASE_URL = "/api/admin/users";
    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Describe : GET /api/admin/users")
    class Describe_with_User_Details_Retrieve{

        CurrentUser testUserDetails;

        User testUser;

        @BeforeEach
        void setUp(){
            userReposiotry.deleteAll();

            User saved = userReposiotry.save(
                    UserUtil.createUser(
                            "testUser@naver.com",
                            passwordEncoder.encode("wjdtn747")
                    )
            );
            saved.updateRole(Role.ADMIN);

            testUserDetails = CurrentUser.from(saved);

            int ITER = 20;

            for(int i = 0 ; i < ITER ; i++){
                User tester = userReposiotry.save(
                        UserUtil.createUser(
                                "testUser%d@naver.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "정수"
                        )
                );
                tester.updateRole(Role.USER);
            }
            for(int i = ITER ; i < 2 * ITER ; i++){
                User tester = userReposiotry.save(
                        UserUtil.createUser(
                                "testUser%d@google.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "유리수"
                        )
                );
                tester.updateRole(Role.ADMIN);
            }
            for(int i = 2 * ITER ; i < 3 * ITER ; i++){
                User tester = userReposiotry.save(
                        UserUtil.createUser(
                                "testUser%d@kakao.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "실수"
                        )
                );
                tester.updateRole(Role.SUPER_ADMIN);
            }
        }


        @Nested
        @DisplayName("Context : 올바른 데이터가 주어지는 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It : 200 응답과 함께 어드민에 의해 유저정보가 성공적으로 조회")
            void It_성공적으로_유저정보_조회_및_응답() throws Exception {

                SecurityContextHolder.getContext().setAuthentication(
                        new TestingAuthenticationToken(
                                testUserDetails,
                                null,
                                "ROLE_ADMIN"

                        )
                );


                // when
                MockHttpServletResponse response = mockMvc.perform(
                        MockMvcRequestBuilders.get(BASE_URL)
                                .param("q", "")
                                .param("role", "")
                                .param("page", "0")
                                .param("size", "100")
                )
                        .andDo(print())
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn().getResponse();

                // then
                String json = response.getContentAsString();
                AdminsUserDetailResponse result = objectMapper.readValue(
                        json,
                        new TypeReference<ApiResponse<AdminsUserDetailResponse>>() {
                        }
                ).data();

                Assertions.assertThat(result.page()).isEqualTo(0);
                Assertions.assertThat(result.size()).isEqualTo(100);
                Assertions.assertThat(result.totalElements()).isEqualTo(61);
                Assertions.assertThat(result.items().size()).isEqualTo(61);
            }
        }

        @Nested
        @DisplayName("Context : 올바르지 않은 인증정보가 주어지는 경우")
        class Context_with_invalid_data{

            @Test
            @DisplayName("It : 인증 정보가 없는 경우 302 에러 도출")
            void It_유저정보_조회_실패_인증_없음() throws Exception {


                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.get(BASE_URL)
                                        .param("q", "")
                                        .param("role", "")
                                        .param("page", "0")
                                        .param("size", "100")
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
            }

            @Test
            @DisplayName("It : ROLE : USER가 조회 시 403 에러 도출")
            void It_유저정보_조회_실패_유저_권한() throws Exception {

                SecurityContextHolder.getContext().setAuthentication(
                        new TestingAuthenticationToken(
                                testUserDetails,
                                null,
                                "ROLE_USER"

                        )
                );


                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.get(BASE_URL)
                                        .param("q", "")
                                        .param("role", "")
                                        .param("page", "0")
                                        .param("size", "100")
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().isForbidden());
            }
        }
    }
}
