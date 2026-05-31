package io.teabag.assetbox.post.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.teabag.assetbox.common.config.JpaConfig;
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

}