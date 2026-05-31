package io.teabag.assetbox.post.controller;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.TestUtil;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(PostController.class)
class PostControllerTests {

    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    PostService postService;


    @Nested
    @DisplayName("게시글 생성")
    class post_생성관련_테스트{
        PostCreateRequest request;
        @BeforeEach
        void setUp(){
            request = TestUtil.postCreateRequestOf();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("201 Created와 성공 응답을 반환한다")
        void createPost() throws Exception {
            // given
            Post savedPost = Post.builder()
                    .title("제목")
                    .content("내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            given(postService.save(any(PostCreateRequest.class)))
                    .willReturn(savedPost);

            // when
            mockMvc.perform(
                            post("/api/posts/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
            // then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("제목"))
                    .andExpect(jsonPath("$.data.content").value("내용"));

//
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - title이 누락되면 400 VALIDATION_FAILED를 반환한다")
        void createPost_fail_when_title_is_blank() throws Exception {
            // given
            PostCreateRequest request = new PostCreateRequest(
                    "",
                    "내용",
                    1L,
                    1L,
                    List.of("spring", "jpa"),
                    null
            );

            // when
            mockMvc.perform(
                            post("/api/posts/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
            // then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.message").value("Validation failed"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - content가 누락되면 400 VALIDATION_FAILED를 반환한다")
        void createPost_fail_when_content_is_blank() throws Exception {
            // given
            PostCreateRequest request = new PostCreateRequest(
                    "제목",
                    "",
                    1L,
                    1L,
                    List.of("spring", "jpa"),
                    null
            );

            // when
            mockMvc.perform(
                            post("/api/posts/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
            //then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.message").value("Validation failed"));
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class post_삭제관련_테스트{
        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("게시글 삭제 요청 시 200 OK와 성공 응답을 반환한다")
        void deletePost_success() throws Exception {
            // given
            Long postId = 1L;

            willDoNothing()
                    .given(postService)
                    .deletePost(postId);

            // when
            mockMvc.perform(
                            delete("/api/posts/{postId}", postId)
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error").isEmpty());

            then(postService)
                    .should()
                    .deletePost(postId);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("게시글 삭제 실패 - 존재하지 않는 게시글이면 404 POST_NOT_FOUND를 반환한다")
        void deletePost_fail_when_post_not_found() throws Exception {
            // given
            Long postId = 999L;

            willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND,"post_not_found"))
                    .given(postService)
                    .deletePost(postId);

            // when
            mockMvc.perform(
                            delete("/api/posts/{postId}", postId)
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));

            then(postService)
                    .should()
                    .deletePost(postId);
        }
    }
}