package io.teabag.assetbox.post.service;

import static org.junit.jupiter.api.Assertions.*;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.repository.TagRepository;
import io.teabag.assetbox.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("게시글 생성 시 태그를 조회하거나 생성한 뒤 게시글을 저장한다")
    void savePost() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                "제목",
                "내용",
                1L,
                1L,
                List.of("spring", "jpa"),
                null
        );

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