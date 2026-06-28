package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SearchUserByAdminResponse;
import io.teabag.assetbox.user.dto.UserUpdateRoleRequest;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.util.UserUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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
                                .param("size", "50")
                )
                        .andDo(print())
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn().getResponse();

                // then
                String json = response.getContentAsString();
                SearchUserByAdminResponse result = objectMapper.readValue(
                        json,
                        new TypeReference<ApiResponse<SearchUserByAdminResponse>>() {
                        }
                ).data();

                Assertions.assertThat(result.page()).isEqualTo(0);
                Assertions.assertThat(result.size()).isEqualTo(50);
                Assertions.assertThat(result.totalElements()).isEqualTo(61);
                Assertions.assertThat(result.items().size()).isEqualTo(50);
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
                                        .param("size", "50")
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
                                        .param("size", "50")
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("Describe : PATCH /api/admin/users/{id}/user")
    class Describe_with_user_role_switch{

        User testUser;
        User tester;

        @BeforeEach
        void setUp(){
            testUser = userReposiotry.save(
                    UserUtil.createUser(
                            "testUser@naver.com",
                            passwordEncoder.encode("123456789")
                    )
            );
            tester = userReposiotry.save(
                    UserUtil.createUser(
                            "testAdmin@naver.com",
                            passwordEncoder.encode("123456789")
                    )
            );
        }

        @Nested
        @DisplayName("Context : Super Admin이 수정하는 경우")
        class Context_with_modify_by_superadmin{

            @Test
            @DisplayName("It : User -> Admin 변환 성공")
            void It_유저_에서_어드민_으로_역할_변환_성공() throws Exception {
                // given
                tester.updateRole(Role.SUPER_ADMIN);
                TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                        CurrentUser.from(
                                tester
                        ),
                        null,
                        "ROLE_" + tester.getRole()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                String json = objectMapper.writeValueAsString(new UserUpdateRoleRequest(Role.ADMIN));

                // when
                mockMvc.perform(
                        MockMvcRequestBuilders.patch(BASE_URL + "/{id}/role",testUser.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                // then
                        .andExpect(MockMvcResultMatchers.status().isOk());

                User founded = userReposiotry.findByEmailOrThrow(testUser.getEmail());
                Assertions.assertThat(founded.getRole()).isEqualTo(Role.ADMIN);
            }
        }

        @Nested
        @DisplayName("Context : 적합한 권한이 없는 경우")
        class Context_with_unvalid_authorities{

            @Test
            @DisplayName("It : 인증정보가 없는 경우 302 응답 도출")
            void It_역할_변환_실패__인증_정보_없음() throws Exception {
                // given
                String json = objectMapper.writeValueAsString(new UserUpdateRoleRequest(Role.ADMIN));

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.patch(BASE_URL + "/{id}/role",testUser.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
            }

            @ParameterizedTest
            @EnumSource(
                    value = Role.class,
                    names = { "SUPER_ADMIN" },
                    mode = EnumSource.Mode.EXCLUDE
            )
            @DisplayName("It : SUPER ADMIN이 아닌 경우 403 예외 발생")
            void It_역할_변환_실패__슈퍼_어드민_아님(Role role) throws Exception {
                // given
                tester.updateRole(role);
                TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                        CurrentUser.from(
                                tester
                        ),
                        null,
                        "ROLE_" + tester.getRole()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // given
                String json = objectMapper.writeValueAsString(new UserUpdateRoleRequest(Role.ADMIN));

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.patch(BASE_URL + "/{id}/role",testUser.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andDo(print());
            }

            @Test
            @DisplayName("It : 본인의 Role을 변경하는 경우 403 예외 발생")
            void It_역할_변환_실패__본인_Role_변경() throws Exception {
                // given
                tester.updateRole(Role.SUPER_ADMIN);
                TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                        CurrentUser.from(
                                tester
                        ),
                        null,
                        "ROLE_" + tester.getRole()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // given
                String json = objectMapper.writeValueAsString(new UserUpdateRoleRequest(Role.ADMIN));

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.patch(BASE_URL + "/{id}/role",tester.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andDo(print());
            }

            @Test
            @DisplayName("It : 존재하지 않은 계정을 변경 시 404 예외 발생")
            void It_역할_변환_실패__존재하지않은_계정() throws Exception {
                // given
                tester.updateRole(Role.SUPER_ADMIN);
                TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                        CurrentUser.from(
                                tester
                        ),
                        null,
                        "ROLE_" + tester.getRole()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // given
                String json = objectMapper.writeValueAsString(new UserUpdateRoleRequest(Role.ADMIN));

                // when
                mockMvc.perform(
                                MockMvcRequestBuilders.patch(BASE_URL + "/{id}/role",423542)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        // then
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andDo(print());
            }




        }

    }
}
