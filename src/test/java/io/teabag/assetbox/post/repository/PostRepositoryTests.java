package io.teabag.assetbox.post.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import io.teabag.assetbox.common.config.JpaConfig;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.util.PostUtil;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@EnableJpaAuditing
class PostRepositoryTests {

    @Autowired
    PostRepository postRepository;

    @Nested
    @DisplayName("게시글 생성")
    class 포스트_저장{
        @Test
        @DisplayName("저장")
        void savePost() {
            // given
            Post post = Post.builder()
                    .title("제목")
                    .content("내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            // when
            Post savedPost = postRepository.save(post);

            // then
            assertThat(savedPost.getId()).isNotNull();
            assertThat(savedPost.getTitle()).isEqualTo("제목");
            assertThat(savedPost.getContent()).isEqualTo("내용");
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class 포스트_삭제{
        @Test
        @DisplayName("soft delete 시 deletedAt이 저장된다")
        void softDeletePost() {
            // given
            Post post = Post.builder()
                    .title("제목")
                    .content("내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            Post savedPost = postRepository.saveAndFlush(post);

            // when
            savedPost.softDelete();
            postRepository.saveAndFlush(savedPost);

            // then
            Post foundPost = postRepository.findByIdOrThrow(savedPost.getId());
            assertThat(foundPost.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class 포스트_수정 {

        @Test
        @DisplayName("게시글의 제목과 내용을 수정할 수 있다")
        void updatePost_success() {
            // given
            Post post = Post.builder()
                    .title("기존 제목")
                    .content("기존 내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            Post savedPost = postRepository.saveAndFlush(post);

            // when
            savedPost.update(
                    "수정 제목",
                    "수정 내용",
                    2L
            );

            postRepository.flush();

            // then
            Post foundPost = postRepository.findByIdOrThrow(savedPost.getId());


            assertThat(foundPost.getTitle()).isEqualTo("수정 제목");
            assertThat(foundPost.getContent()).isEqualTo("수정 내용");
            assertThat(foundPost.getCategoryId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class 포스트_조회 {

        @Nested
        @DisplayName("게시글 단건 조회")
        class 포스트_단건_조회 {
            @Test
            @DisplayName("게시글이 존재하면 findByIdOrThrow로 조회할 수 있다")
            void findByIdOrThrow_success() {
                // given
                Post post = Post.builder()
                        .title("제목")
                        .content("내용")
                        .authorId(1L)
                        .categoryId(1L)
                        .build();

                Post savedPost = postRepository.saveAndFlush(post);

                // when
                Post foundPost = postRepository.findByIdOrThrow(savedPost.getId());

                // then
                assertThat(foundPost.getId()).isEqualTo(savedPost.getId());
                assertThat(foundPost.getTitle()).isEqualTo("제목");
            }

            @Test
            @DisplayName("게시글이 없으면 POST_NOT_FOUND 예외가 발생한다")
            void findByIdOrThrow_fail_when_not_found() {
                // given
                Long postId = 999L;

                // when & then
                assertThatThrownBy(() -> postRepository.findByIdOrThrow(postId))
                        .isInstanceOf(BusinessException.class);
            }
        }

        @Nested
        @DisplayName("게시글 다건 조회")
        class 포스트_다건_조회 {
            @Nested
            @DisplayName("게시글 목록 조회")
            class GetPosts {

                @Test
                @DisplayName("삭제되지 않은 게시글만 Slice로 조회한다")
                void getPosts_success() {
                    // given
                    Post post1 = Post.builder()
                            .title("제목1")
                            .content("내용1")
                            .authorId(1L)
                            .categoryId(1L)
                            .build();

                    Post post2 = Post.builder()
                            .title("제목2")
                            .content("내용2")
                            .authorId(2L)
                            .categoryId(1L)
                            .build();

                    Post deletedPost = Post.builder()
                            .title("삭제된 제목")
                            .content("삭제된 내용")
                            .authorId(3L)
                            .categoryId(1L)
                            .build();

                    deletedPost.softDelete();

                    postRepository.save(post1);
                    postRepository.save(post2);
                    postRepository.save(deletedPost);
                    postRepository.flush();

                    Pageable pageable = PageRequest.of(
                            0,
                            2,
                            Sort.by(Sort.Direction.DESC, "createdAt")
                    );

                    // when
                    Slice<Post> result =
                            postRepository.findAllByDeletedAtIsNull(pageable);

                    // then
                    assertThat(result.getContent()).hasSize(2);

                    assertThat(result.getContent())
                            .extracting(Post::getTitle)
                            .doesNotContain("삭제된 제목");

                    assertThat(result.getNumber()).isEqualTo(0);
                    assertThat(result.getSize()).isEqualTo(2);
                }
            }
        }
    }

    @Nested
    @DisplayName("Description : getCountByRequesterId() 메서드에")
    class Describe_with_getCountByRequester{

        @BeforeEach
        void setUp(){
            postRepository.save(
                    PostUtil.create(1L,1L,1L)
            );
            postRepository.save(
                    PostUtil.create(1L,1L,1L)
            );
            postRepository.save(
                    PostUtil.create(1L,1L,1L)
            );
        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어지는 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It : 성공적으로 작성한 게시글의 수를 조회")
            void It_테스트_성공(){
                // given
                // 다른 사용자가 작성한 게시글이 존재하더라도 작성자 ID의 게시글만 조회
                postRepository.save(
                        PostUtil.create(2L,1L,1L)
                );

                // when
                Integer count = postRepository.getCountByRequesterId(1L);

                // then
                Assertions.assertNotNull(count);
                Assertions.assertEquals(count, 3);
            }

        }

    }

}