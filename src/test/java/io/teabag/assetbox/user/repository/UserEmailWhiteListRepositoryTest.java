package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostLike;
import io.teabag.assetbox.post.repository.PostLikeRepository;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SearchUserByAdminResponse;
import io.teabag.assetbox.user.dto.directory.SearchUserResponse;
import io.teabag.assetbox.user.dto.UserDetailsResponse;
import io.teabag.assetbox.user.dto.directory.UserInfoResponse;
import io.teabag.assetbox.util.PostUtil;
import io.teabag.assetbox.util.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserEmailWhiteListRepositoryTest {

    @Autowired
    UserEmailRepository userEmailRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    PostRepository postRepository;
    @Autowired
    PostLikeRepository postLikeRepository;

    @Nested
    @DisplayName("Describe : UserEmailRepository의 findUserByAdmin() 메서드에")
    class Describe_with_findUserByAdmin{

        User testUser;

        @BeforeEach
        void setUp() {
            userRepository.deleteAll();

            int ITER = 20;

            for(int i = 0 ; i < ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUser(
                                "testUser%d@naver.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "정수"
                        )
                );
                tester.updateRole(Role.USER);
            }
            for(int i = ITER ; i < 2 * ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUser(
                                "testUser%d@google.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "유리수"
                        )
                );
                tester.updateRole(Role.ADMIN);
            }
            for(int i = 2 * ITER ; i < 3 * ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUser(
                                "testUser%d@kakao.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "실수"
                        )
                );
                tester.updateRole(Role.SUPER_ADMIN);
            }

            testUser = userRepository.findByEmailOrThrow("testUser1@naver.com");


            for(int i = 0 ; i < ITER*2 ; i++){
                Post savedNormalPost = postRepository.save(
                        PostUtil.create(
                                testUser.getId(),
                                1L,
                                1L
                        )
                );

                postLikeRepository.save(
                        new PostLike(
                                testUser.getId(),
                                savedNormalPost.getId()
                        )
                );


            }

        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어지는 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It : 어드민에 의한 유저 검색 성공 : 아무 조건 없는 경우")
            void It_유저_검색_성공(){
                // when
                SearchUserByAdminResponse founded = userEmailRepository.findUserByAdmin(
                        null,
                        null,
                        PageRequest.of(0, 20)
                );

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(20);
                Assertions.assertThat(founded.totalElements()).isEqualTo(60);
                Assertions.assertThat(founded.totalPages()).isEqualTo(3);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                UserDetailsResponse response = founded.items().stream().filter(
                        item -> item.getEmail().equals(testUser.getEmail())
                ).findFirst().orElse(null);

                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.getTotalLikes()).isEqualTo(40);
                Assertions.assertThat(response.getPostCount()).isEqualTo(40);

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }

            @Test
            @DisplayName("It : 어드민에 의한 유저 검색 성공 : 역할 입력 - 유저")
            void It_유저_검색_성공__역할_유저(){
                // when
                SearchUserByAdminResponse founded = userEmailRepository.findUserByAdmin(
                        Role.USER.name(),
                        null,
                        PageRequest.of(0, 30)
                );

                for(UserDetailsResponse response :founded.items()){
                    log.info(response.getEmail());
                }

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(30);
                Assertions.assertThat(founded.totalElements()).isEqualTo(20);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }

            @Test
            @DisplayName("It : 어드민에 의한 유저 검색 성공 : 검색어 입력 - 이메일")
            void It_유저_검색_성공__검색어_이메일(){
                // when
                SearchUserByAdminResponse founded = userEmailRepository.findUserByAdmin(
                        null,
                        "kakao",
                        PageRequest.of(0, 30)
                );


                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(30);
                Assertions.assertThat(founded.totalElements()).isEqualTo(20);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }

            @Test
            @DisplayName("It : 어드민에 의한 유저 검색 성공 : 검색어 입력 - 이름")
            void It_유저_검색_성공__검색어_이름(){
                // when
                SearchUserByAdminResponse founded = userEmailRepository.findUserByAdmin(
                        null,
                        "실수",
                        PageRequest.of(0, 30)
                );

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(30);
                Assertions.assertThat(founded.totalElements()).isEqualTo(20);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }

        }


        @Nested
        @DisplayName("Context : 올바르지 않은 데이터가 주어지는 경우")
        class Context_with_invalid_data{

            @Test
            @DisplayName("It: 검색어에 해당하는 유저가 없음.")
            void It_테스트_실패__검색어에_해당하는_유저_없음(){
                // when
                SearchUserByAdminResponse founded = userEmailRepository.findUserByAdmin(
                        null,
                        "잘못된 키워드",
                        PageRequest.of(0, 30)
                );

                Assertions.assertThat(founded.items().size()).isEqualTo(0);
            }

        }
    }

    @Nested
    @DisplayName("Describe : UserEmailRepository의 findUser() 메서드에")
    class Describe_with_findUser{
        User testUser;

        @BeforeEach
        void setUp() {
            userRepository.deleteAll();

            int ITER = 20;

            for(int i = 0 ; i < ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUser(
                                "testUser%d@naver.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "정수"
                        )
                );
                tester.updateRole(Role.USER);
            }
            for(int i = ITER ; i < 2 * ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUser(
                                "testUser%d@google.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "유리수"
                        )
                );
                tester.updateRole(Role.ADMIN);
            }
            for(int i = 2 * ITER ; i < 3 * ITER ; i++){
                User tester = userRepository.save(
                        UserUtil.createUserWithNickname(
                                "testUser%d@kakao.com".formatted(i),
                                passwordEncoder.encode("wjdtn3902"),
                                i + "실수",
                                i + "별명"
                        )
                );
                tester.updateRole(Role.SUPER_ADMIN);
            }

            testUser = userRepository.findByEmailOrThrow("testUser1@naver.com");


            for(int i = 0 ; i < ITER*2 ; i++){
                Post savedNormalPost = postRepository.save(
                        PostUtil.create(
                                testUser.getId(),
                                1L,
                                1L
                        )
                );

                postLikeRepository.save(
                        new PostLike(
                                testUser.getId(),
                                savedNormalPost.getId()
                        )
                );

            }

        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어지는 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It : 유저 검색 성공 : 아무 조건 없는 경우")
            void It_유저_검색_성공(){
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "postCount",
                        "desc",
                        null,
                        null,
                        PageRequest.of(0, 20)
                );

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(20);
                Assertions.assertThat(founded.totalElements()).isEqualTo(60);
                Assertions.assertThat(founded.totalPages()).isEqualTo(3);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                UserInfoResponse response = founded.items().stream().filter(
                        item -> item.getName().equals(testUser.getName())
                ).findFirst().orElse(null);

                Assertions.assertThat(response).isNotNull();
                Assertions.assertThat(response.getTotalLikes()).isEqualTo(40);
                Assertions.assertThat(response.getPostCount()).isEqualTo(40);

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }


            @Test
            @DisplayName("It : 유저 검색 성공 : 전공 입력 - TA")
            void It_유저_검색_성공__역할_유저(){
                // given
                userRepository.save(
                        UserUtil.createUserWithMajor(
                                "testUse@google.com",
                                passwordEncoder.encode("wjdtn3902"),
                                "유리수",
                                Major.TA
                        )
                );

                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "postCount",
                        "desc",
                        Major.TA.name(),
                        null,
                        PageRequest.of(0, 20)
                );

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(20);
                Assertions.assertThat(founded.totalElements()).isEqualTo(1);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(1);
            }


            @Test
            @DisplayName("It : 유저 검색 성공 : 검색어 입력 - 이름")
            void It_유저_검색_성공__검색어_이름(){
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "postCount",
                        "desc",
                        null,
                        "11정수",
                        PageRequest.of(0, 30)
                );


                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(30);
                Assertions.assertThat(founded.totalElements()).isEqualTo(1);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(1);
            }

            @Test
            @DisplayName("It : 유저 검색 성공 : 검색어 입력 - 별명")
            void It_유저_검색_성공__검색어_별명(){
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "postCount",
                        "desc",
                        null,
                        "별명",
                        PageRequest.of(0, 30)
                );

                // then
                Assertions.assertThat(founded.items()).isNotNull();
                Assertions.assertThat(founded.page()).isEqualTo(0);
                Assertions.assertThat(founded.size()).isEqualTo(30);
                Assertions.assertThat(founded.totalElements()).isEqualTo(20);
                Assertions.assertThat(founded.totalPages()).isEqualTo(1);
                Assertions.assertThat(founded.first()).isTrue();
                Assertions.assertThat(founded.last()).isFalse();

                Assertions.assertThat(founded.items().size()).isEqualTo(20);
            }

            @Test
            @DisplayName("It : 유저 검색 성공 : 별명 오름차순")
            void It_유저_검색_성공__별명_오름차순(){
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "nickname",
                        "",
                        null,
                        null,
                        PageRequest.of(0, 30)
                );

                for(UserInfoResponse response : founded.items()){
                    log.info(response.getNickname());
                }

                // then
                Assertions.assertThat(founded.items().get(0).getNickname()).isEqualTo("40별명");
            }

            @Test
            @DisplayName("It : 유저 검색 성공 : 좋아요_수 내림차순")
            void It_유저_검색_성공__좋아요_수_내림차순(){
                log.info(testUser.getName());
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "totalLikes",
                        "desc",
                        null,
                        null,
                        PageRequest.of(0, 30)
                );

                // then
                Assertions.assertThat(founded.items().get(0).getId()).isEqualTo(testUser.getId());
                Assertions.assertThat(founded.totalElements()).isEqualTo(60);
            }

            @Test
            @DisplayName("It : 유저 검색 성공 : 게시물수 내림차순")
            void It_유저_검색_성공__게시물_수_내림차순(){
                log.info(testUser.getName());
                // when
                SearchUserResponse founded = userEmailRepository.findUser(
                        "postCount",
                        "desc",
                        null,
                        null,
                        PageRequest.of(0, 30)
                );

                // then
                Assertions.assertThat(founded.items().get(0).getId()).isEqualTo(testUser.getId());
                Assertions.assertThat(founded.totalElements()).isEqualTo(60);
            }
        }
    }
}