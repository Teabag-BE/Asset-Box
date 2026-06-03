package io.teabag.assetbox.post.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import io.teabag.assetbox.TestUtil;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.repository.TagRepository;
import io.teabag.assetbox.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@Transactional
class PostServiceTests {

    @Mock
    TagRepository tagRepository;

    @Mock
    PostRepository postRepository;

    @InjectMocks
    PostService postService;

    @Nested
    @DisplayName("게시글 생성 관련")
    class postCreate{
        @Test
        @DisplayName("생성 시 태그를 조회하거나 생성한 뒤 게시글을 저장한다")
        void savePost() {
            // given
            PostCreateRequest request = TestUtil.postCreateRequestOf();

            Tag springTag = new Tag("spring");
            Tag jpaTag = new Tag("jpa");

            given(tagRepository.findByName("spring"))
                    .willReturn(Optional.of(springTag));

            given(tagRepository.findByName("jpa"))
                    .willReturn(Optional.empty());

            given(tagRepository.save(any(Tag.class)))
                    .willReturn(jpaTag);

            given(postRepository.save(any(Post.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Post savedPost = postService.save(request);

            // then
            assertThat(savedPost.getTitle()).isEqualTo("제목");
            assertThat(savedPost.getContent()).isEqualTo("내용");

            then(tagRepository).should().findByName("spring");
            then(tagRepository).should().findByName("jpa");
            then(tagRepository).should().save(any(Tag.class));
            then(postRepository).should().save(any(Post.class));
        }

    }

    @Nested
    @DisplayName("게시글 삭제 관련")
    class postDelete{
        @Test
        @DisplayName("삭제 시 실제 삭제하지 않고 deletedAt을 채운다")
        void deletePost_success() {
            // given
            Long postId = 1L;

            Post post = Post.builder()
                    .title("제목")
                    .content("내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            // when
            postService.deletePost(postId);

            // then
            assertThat(post.getDeletedAt()).isNotNull();

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(postRepository)
                    .should(never())
                    .delete(any(Post.class));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 삭제 시 예외가 발생")
        void deletePost_fail_when_post_not_found() {
            // given
            Long postId = 999L;

            given(postRepository.findByIdOrThrow(postId))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when
            assertThatThrownBy(() -> postService.deletePost(postId))
                    .isInstanceOf(BusinessException.class);


            //then
            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);
            then(postRepository)
                    .should(never())
                    .delete(any(Post.class));
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        PostUpdateRequest request;

        @BeforeEach
        void setUp(){
            request = TestUtil.postUpdateRequestOf();
        }

        @Test
        @DisplayName("게시글이 존재하면 제목, 내용, 카테고리, 태그를 수정한다")
        void updatePost_success() {
            // given
            Long postId = 1L;

            Post post = Post.builder()
                    .title("기존 제목")
                    .content("기존 내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            Tag springTag = new Tag("spring");
            Tag jpaTag = new Tag("jpa");

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            given(tagRepository.findByName("spring"))
                    .willReturn(Optional.of(springTag));

            given(tagRepository.findByName("jpa"))
                    .willReturn(Optional.empty());

            given(tagRepository.save(any(Tag.class)))
                    .willReturn(jpaTag);

            // when
            Post updatedPost = postService.updatePost(postId, request);

            // then
            assertThat(updatedPost.getTitle()).isEqualTo("수정 제목");
            assertThat(updatedPost.getContent()).isEqualTo("수정 내용");
            assertThat(updatedPost.getCategoryId()).isEqualTo(1L);

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(tagRepository)
                    .should()
                    .findByName("spring");

            then(tagRepository)
                    .should()
                    .findByName("jpa");

            then(tagRepository)
                    .should()
                    .save(any(Tag.class));
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외를 발생시킨다")
        void updatePost_fail_when_post_not_found() {
            // given
            Long postId = 999L;

            given(postRepository.findByIdOrThrow(postId))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> postService.updatePost(postId, request))
                    .isInstanceOf(BusinessException.class);

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(tagRepository)
                    .shouldHaveNoInteractions();
        }
    }




}