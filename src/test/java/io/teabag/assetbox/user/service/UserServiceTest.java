package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.repository.UserEmailRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
    UserRepository realtUserRepository;
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

            @Test
            @DisplayName("It: 유저가 성공적으로 생성")
            void It_유저_성공적으로_생성(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        "wjdtn0619",
                        "일정수"
                );
                EmailWhiteList founded = userReposiotry.emailWhiteListSave(
                        new EmailWhiteList("이정수", Major.BACK_END, "testuser1@naver.com")
                );
                founded.switchVerified();

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
            @DisplayName("It: 화이트리스트에 없는 이메일 계정은 가입 실패")
            void It_유저_화이트리스트_없는_경우_가입_실패(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        USER_PASSWORD,
                        "일정수"
                );
                // when
                Assertions.assertThatThrownBy(
                                // when
                                ()-> userService.signup(request)
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.EMAIL_NOT_ON_WHITELIST.getDescription());
            }

            @Test
            @DisplayName("It: 화이트리스트에서 인증 대기인 이메일 계정은 가입 실패")
            void It_유저_화이트리스트_통과_안된_경우_가입_실패(){
                // given
                SignupRequest request = UserUtil.createUserCreateRequest(
                        "testuser1@naver.com",
                        USER_PASSWORD,
                        "일정수"
                );
                EmailWhiteList founded = userReposiotry.emailWhiteListSave(
                        new EmailWhiteList("이정수", Major.BACK_END, "testuser1@naver.com")
                );
                // when
                Assertions.assertThatThrownBy(
                                // when
                                ()-> userService.signup(request)
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.EMAIL_NOT_VERIFIED.getDescription());

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
    @DisplayName("Describe: getMyInfo() 메서드에서")
    class Describe_with_get_my_info {
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
        @DisplayName("Context: 등록된 사용자의 정보를 조회하는 경우")
        class Context_with_registered_user{

            @Test
            @DisplayName("It: 해당 유저 정보가 정상적으로 조회되고, 반환 된다.")
            void It_내_정보_조회_성공_및_반환() {
                // given & when
                MyInfoResponse response = userService.getMyInfo(USER_EMAIL);

                // then
                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.id()).isEqualTo(testUser.getId());
                Assertions.assertThat(response.email()).isEqualTo(USER_EMAIL);
                Assertions.assertThat(response.name()).isEqualTo(testUser.getName());
                Assertions.assertThat(response.nickname()).isEqualTo(testUser.getNickname());
                Assertions.assertThat(response.major()).isEqualTo(testUser.getMajor().name());
                Assertions.assertThat(response.role()).isEqualTo(testUser.getRole().name());
            }

        }

        @Nested
        @DisplayName("Context: 등록되지 않은 사용자의 정보를 조회하는 경우")
        class Context_with_unknown_user{
            @Test
            @DisplayName("It: 정보가 조회되지 않고, USER_NOT_FOUND 에러를 던진다")
            void It_내_정보_조회_실패() {
                // given
                String unknownEmail = "유효하지않은@사용자.com";

                // when & then
                Assertions.assertThatThrownBy(
                                () -> userService.getMyInfo(unknownEmail)
                        )
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getDescription());

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

                SearchUserByAdminResponse founded = userService.getUserDetailsByAdmin(
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

    @Nested
    @DisplayName("Describe : switchRole() 메서드에서")
    class Describe_with_switchRole{

        User testUser;
        User tester;


        @BeforeEach
        void setUp(){
            testUser = realtUserRepository.save(
                    UserUtil.createUser(
                            "testUser@naver.com",
                            passwordEncoder.encode("123456789")
                    )
            );
            tester = realtUserRepository.save(
                    UserUtil.createUser(
                            "testAdmin@naver.com",
                            passwordEncoder.encode("123456789")
                    )
            );
        }

        @Nested
        @DisplayName("Context : Super Admin이 수정하는 경우")
        class Context_with_by_superadmin{

            @Test
            @DisplayName("It : 유저 -> 어드민 역할 전환 성공")
            void It_역할_전환_성공_유저_어드민(){
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

                // when
                userService.switchRole(
                        testUser.getId(),
                        tester.getEmail(),
                        Role.ADMIN
                );

                // then
                User founded = userReposiotry.findByIdOrThrow(testUser.getId());
                Assertions.assertThat(founded.getRole()).isEqualTo(Role.ADMIN);
            }

            @Test
            @DisplayName("It : 어드민 -> 유저 역할 전환 성공")
            void It_역할_전환_성공_어드민_유저(){
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

                testUser.updateRole(Role.ADMIN);

                // when
                userService.switchRole(
                        testUser.getId(),
                        tester.getEmail(),
                        Role.USER
                );

                // then
                User founded = userReposiotry.findByIdOrThrow(testUser.getId());
                Assertions.assertThat(founded.getRole()).isEqualTo(Role.USER);
            }

        }

        @Nested
        @DisplayName("Context : 인증이 없거나 Super Admin이 아닌 경우")
        class Context_with_by_not_superadmin {
            @Test
            @DisplayName("It : 인증이 없는 경우 전환 실패")
            void It_역할_전환_실패_인증_없음(){
                // when
                Assertions.assertThatThrownBy(
                        ()-> userService.switchRole(
                                testUser.getId(),
                                tester.getEmail(),
                                Role.ADMIN
                        )
                )
                        // then
                        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            }

            @Test
            @DisplayName("It : 본인의 역할을 변경하는 경우")
            void It_역할_전환_실패_본인_역할_변경(){
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

                // when
                Assertions.assertThatThrownBy(
                                ()-> userService.switchRole(
                                        tester.getId(),
                                        tester.getEmail(),
                                        Role.ADMIN
                                )
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.FORBIDDEN_SELF_ROLE_CHANGE.getDescription());
            }

            @ParameterizedTest
            @EnumSource(
                value = Role.class,
                names = { "SUPER_ADMIN" },
                mode = EnumSource.Mode.EXCLUDE
            )
            @DisplayName("It : 슈퍼어드민이 아닌 경우 전환 실패")
            void It_역할_전환_실패_슈퍼어드민_아님(Role role){
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

                // when
                Assertions.assertThatThrownBy(
                                ()-> userService.switchRole(
                                        testUser.getId(),
                                        tester.getEmail(),
                                        Role.ADMIN
                                )
                        )
                        // then
                        .isInstanceOf(AuthorizationDeniedException.class);
            }
        }

        @Nested
        @DisplayName("Context : 잘못된 데이터가 주어지는 경우")
        class Context_with_invalid_data {
            @Test
            @DisplayName("It : 똑같은 역할을 전달 시 실패")
            void It_역할_전환_실패_동일_역할(){
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

                // when
                Assertions.assertThatThrownBy(
                                ()-> userService.switchRole(
                                        testUser.getId(),
                                        tester.getEmail(),
                                        Role.USER
                                )
                        )
                        // then
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.CAN_NOT_SWITCH_TO_SAME_ROLE.getDescription());
            }
        }
    }

    @Nested
    @DisplayName("Describe: getUserProfile() 메서드에서")
    class Describe_with_get_user_profile {
        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456789";


        @BeforeEach
        void setUp(){
            testUser = User.builder()
                    .email(USER_EMAIL)
                    .name("테스트")
                    .nickname("닉네임")
                    .major(Major.BACK_END)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .build();
            userReposiotry.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 일반 USER 권한으로 등록된 사용자의 정보를 조회하는 경우")
        class Context_with_normal_user_get_registered_user{

            @Test
            @DisplayName("It: 해당 유저 정보가 정상적으로 조회되고, 이메일을 제외한 정보가 반환 된다.")
            void It_이메일_제외_조회_성공_및_반환() {
                // given & when
                UserProfileResponse response = userService.getUserProfile(testUser.getId(), testUser.getRole());

                // then
                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.id()).isEqualTo(testUser.getId());
                Assertions.assertThat(response.email()).isNull();
                Assertions.assertThat(response.publicEmail()).isEqualTo(testUser.getPublicEmail());
                Assertions.assertThat(response.name()).isEqualTo(testUser.getName());
                Assertions.assertThat(response.nickname()).isEqualTo(testUser.getNickname());
                Assertions.assertThat(response.major()).isEqualTo(testUser.getMajor().name());
                Assertions.assertThat(response.role()).isEqualTo(testUser.getRole().name());
            }

        }

        @Nested
        @DisplayName("Context: 관리자 권한으로 등록된 사용자의 정보를 조회하는 경우")
        class Context_with_admin_user_get_registered_user{

            @Test
            @DisplayName("It: 이메일을 포함한 해당 유저 정보가 정상적으로 조회된다.")
            void It_이메일_포함_조회_성공_및_반환() {
                // given
                testUser.updateRole(Role.ADMIN);

                // when
                UserProfileResponse response = userService.getUserProfile(testUser.getId(), testUser.getRole());

                // then
                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.id()).isEqualTo(testUser.getId());
                Assertions.assertThat(response.email()).isEqualTo(testUser.getEmail());
                Assertions.assertThat(response.publicEmail()).isEqualTo(testUser.getPublicEmail());
                Assertions.assertThat(response.name()).isEqualTo(testUser.getName());
                Assertions.assertThat(response.nickname()).isEqualTo(testUser.getNickname());
                Assertions.assertThat(response.major()).isEqualTo(testUser.getMajor().name());
                Assertions.assertThat(response.role()).isEqualTo(testUser.getRole().name());
            }
        }

        @Nested
        @DisplayName("Context: 등록되지 않은 사용자의 정보를 조회하는 경우")
        class Context_with_unknown_user{
            @Test
            @DisplayName("It: 정보가 조회되지 않고, USER_NOT_FOUND 에러를 던진다")
            void It_존재하지_않는_유저_정보_조회_실패() {
                // given
                Long invalidId = 99999L;

                // when & then
                Assertions.assertThatThrownBy(
                                () -> userService.getUserProfile(invalidId, testUser.getRole())
                        )
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getDescription());

            }

        }
    }

    @Nested
    @DisplayName("Describe: updateMyInfo() 메서드에서")
    class Describe_with_update_my_info {
        User testUser;
        String USER_EMAIL = "testuser2@naver.com";
        String USER_PASSWORD = "123456789";


        @BeforeEach
        void setUp(){
            testUser = User.builder()
                    .email(USER_EMAIL)
                    .name("테스트")
                    .nickname("원본닉네임")
                    .major(Major.BACK_END)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .build();
            userReposiotry.userSave(testUser);
        }

        @Nested
        @DisplayName("Context: 정상적으로 모든 정보를 수정하는 경우")
        class Context_with_update_full_valid_data{

            @Test
            @DisplayName("It: 정상적으로 해당 정보를 수정하고, 수정된 정보를 반환한다")
            void It_모든_정보_수정_성공_및_반환() {
                // given
                UserUpdateRequest request = new UserUpdateRequest(
                        "수정닉네임",
                        "TA",
                        "update@naver.com",
                        "새로운 자기소개"
                );

                // when
                MyInfoResponse response = userService.updateMyInfo(testUser.getEmail(), request);

                // then
                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.id()).isEqualTo(testUser.getId());
                Assertions.assertThat(response.email()).isEqualTo(testUser.getEmail());
                Assertions.assertThat(response.publicEmail()).isEqualTo(request.publicEmail());
                Assertions.assertThat(response.name()).isEqualTo(testUser.getName());
                Assertions.assertThat(response.nickname()).isEqualTo(request.nickname());
                Assertions.assertThat(response.major()).isEqualTo(request.major());
                Assertions.assertThat(response.description()).isEqualTo(request.description());
                Assertions.assertThat(response.role()).isEqualTo(testUser.getRole().name());
            }

        }

        @Nested
        @DisplayName("Context: 정상적으로 일부 null값이 포함된 정보를 수정하는 경우")
        class Context_with_update_not_full_valid_data{

            @Test
            @DisplayName("It: 정상적으로 null 값을 제외한 해당 정보를 수정하고, 수정된 정보를 반환한다")
            void It_null값_제외_정보_수정_성공_및_반환() {
                // given
                UserUpdateRequest request = new UserUpdateRequest(
                        "수정닉네임",
                        null,
                        null,
                        "새로운 자기소개"
                );

                // when
                MyInfoResponse response = userService.updateMyInfo(testUser.getEmail(), request);

                // then
                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.id()).isEqualTo(testUser.getId());
                Assertions.assertThat(response.email()).isEqualTo(testUser.getEmail());
                Assertions.assertThat(response.publicEmail()).isEqualTo(testUser.getPublicEmail());
                Assertions.assertThat(response.name()).isEqualTo(testUser.getName());
                Assertions.assertThat(response.nickname()).isEqualTo(request.nickname());
                Assertions.assertThat(response.major()).isEqualTo(testUser.getMajor().name());
                Assertions.assertThat(response.description()).isEqualTo(request.description());
                Assertions.assertThat(response.role()).isEqualTo(testUser.getRole().name());
            }

        }

        @Nested
        @DisplayName("Context: 등록되지 않은 이메일이 들어온 경우")
        class Context_with_update_unknown_email{

            @Test
            @DisplayName("It: USER_NOT_FOUND 예외가 발생한다")
            void It_user_not_found() {
                // given
                String unknownEmail = "유효하지않은@사용자.com";

                UserUpdateRequest request = new UserUpdateRequest(
                        "수정닉네임",
                        "TA",
                        "update@naver.com",
                        "새로운 자기소개"
                );

                // when & then
                Assertions.assertThatThrownBy(
                                () -> userService.updateMyInfo(unknownEmail, request)
                        )
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getDescription());
            }

        }
    }

}
