package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.common.config.JpaConfig;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostLike;
import io.teabag.assetbox.util.PostUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@Import({JpaConfig.class})
class PostLikeRepositoryTest {

    @Autowired
    PostLikeRepository postLikeRepository;
    @Autowired
    PostRepository postRepository;

    @Nested
    @DisplayName("Describe : PostLikeRepository의 getCountByRequesterId() 메서드에 대해")
    class Describe_with_getCountByRequesterId{

        @BeforeEach
        void setUp(){
            Post savedPost1 = postRepository.save(
                    PostUtil.create(
                            1L,
                            1L,
                            1L
                    )
            );
            Post savedPost2 = postRepository.save(
                    PostUtil.create(
                            1L,
                            1L,
                            1L
                    )
            );
            postLikeRepository.save(
                    PostLike.builder()
                            .userId(savedPost1.getId())
                            .postId(1L)
                            .build()
            );
            postLikeRepository.save(
                    PostLike.builder()
                            .userId(savedPost2.getId())
                            .postId(2L)
                            .build()
            );
        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_validdata{

            @Test
            @DisplayName("It : 성공적으로 Post의 좋아요 수를 조회한다.")
            void It_좋아요_수_카운트_성공(){
                // when
                Integer count = postLikeRepository.getCountByUserId(1L);

                // then
                Assertions.assertThat(count).isNotNull();
                Assertions.assertThat(count).isEqualTo(2);
            }
        }

    }

}