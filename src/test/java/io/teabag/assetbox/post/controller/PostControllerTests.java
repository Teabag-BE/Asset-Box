package io.teabag.assetbox.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.util.TestUtil;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

@WebMvcTest(PostController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTests {

    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    PostService postService;

    @MockitoBean
    JwtFilter jwtFilter;


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
            request = new PostCreateRequest(
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
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                    .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - content가 누락되면 400 VALIDATION_FAILED를 반환한다")
        void createPost_fail_when_content_is_blank() throws Exception {
            // given
            request = new PostCreateRequest(
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
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                    .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
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

    @Nested
    @DisplayName("게시글 수정")
    class post_수정관련_테스트{

        PostUpdateRequest request;

        @BeforeEach
        void setUp(){
            request = TestUtil.postUpdateRequestOf();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("게시글 수정 요청 시 200 OK와 성공 응답을 반환한다")
        void updatePost_success() throws Exception {
            // given
            Long postId = 1L;
            Post updatedPost = Post.builder()
                    .title("수정 제목")
                    .content("수정 내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            given(postService.updatePost(eq(postId), any(PostUpdateRequest.class)))
                    .willReturn(updatedPost);

            // when & then
            mockMvc.perform(
                            put("/api/posts/{postId}", postId)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("수정 제목"))
                    .andExpect(jsonPath("$.data.content").value("수정 내용"));

            then(postService)
                    .should()
                    .updatePost(eq(postId), any(PostUpdateRequest.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("존재하지 않는 게시글 수정 시 404 POST_NOT_FOUND를 반환한다")
        void updatePost_fail_when_post_not_found() throws Exception {
            // given
            Long postId = 999L;

            given(postService.updatePost(eq(postId), any(PostUpdateRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND,"POST_NOT_FOUND"));

            // when & then
            mockMvc.perform(
                            put("/api/posts/{postId}", postId)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("게시물 조회")
    class post_조회관련_테스트 {
        @Nested
        @DisplayName("게시글 단건 조회")
        class GetPost {

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("게시글 단건 조회 성공")
            void getPost_success() throws Exception {
                // given
                Long postId = 1L;

                Post post = Post.builder()
                        .title("제목")
                        .content("내용")
                        .authorId(1L)
                        .categoryId(1L)
                        .build();

                given(postService.getPost(postId))
                        .willReturn(post);

                // when & then
                mockMvc.perform(
                                get("/api/posts/{postId}", postId)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.title").value("제목"));

                then(postService).should().getPost(postId);
            }

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("게시글이 없으면 404 POST_NOT_FOUND")
            void getPost_fail_when_not_found() throws Exception {
                // given
                Long postId = 999L;

                given(postService.getPost(postId))
                        .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

                // when
                mockMvc.perform(
                                get("/api/posts/{postId}", postId)
                        )
                        // then
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
            }
        }

        @Nested
        @DisplayName("게시글 다건 조회")
        class GetPosts {

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("게시글 목록을 조회할 수 있다")
            void getPosts_success() throws Exception {
                // given
                Pageable pageable = PageRequest.of(
                        0,
                        2,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

                List<Post> posts = List.of(
                        Post.builder()
                                .title("제목1")
                                .content("내용1")
                                .authorId(1L)
                                .categoryId(1L)
                                .build(),
                        Post.builder()
                                .title("제목2")
                                .content("내용2")
                                .authorId(2L)
                                .categoryId(1L)
                                .build()
                );

                Slice<Post> slice = new SliceImpl<>(
                        posts,
                        pageable,
                        true
                );

                given(postService.getPosts(any(Pageable.class)))
                        .willReturn(slice);

                // when
                mockMvc.perform(
                                get("/api/posts")
                                        .param("page", "0")
                                        .param("size", "2")
                                        .param("sort", "createdAt,desc")
                        )
                //then
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.items.length()").value(2))
                        .andExpect(jsonPath("$.data.items[0].title").value("제목1"))
                        .andExpect(jsonPath("$.data.items[1].title").value("제목2"))
                        .andExpect(jsonPath("$.data.page").value(0))
                        .andExpect(jsonPath("$.data.size").value(2))
                        .andExpect(jsonPath("$.data.hasNext").value(true));

                then(postService)
                        .should()
                        .getPosts(any(Pageable.class));
            }
        }

    }
}