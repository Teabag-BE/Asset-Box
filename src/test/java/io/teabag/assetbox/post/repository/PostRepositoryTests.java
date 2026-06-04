package io.teabag.assetbox.post.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import io.teabag.assetbox.common.config.JpaConfig;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
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
                @DisplayName("게시글 전체 목록을 조회할 수 있다")
                void getPosts_success() {

                    // given
                    Post post1 = postRepository.save(
                            Post.builder()
                                    .title("제목1")
                                    .content("내용1")
                                    .authorId(1L)
                                    .categoryId(1L)
                                    .build()
                    );

                    Post post2 = postRepository.save(
                            Post.builder()
                                    .title("제목2")
                                    .content("내용2")
                                    .authorId(1L)
                                    .categoryId(1L)
                                    .build()
                    );

                    // when
                    List<Post> posts = postRepository.findAll();

                    // then
                    assertThat(posts).hasSize(2);

                    assertThat(posts)
                            .extracting(Post::getTitle)
                            .contains("제목1", "제목2");
                }
            }
        }
    }

}