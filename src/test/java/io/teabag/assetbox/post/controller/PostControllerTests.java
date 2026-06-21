package io.teabag.assetbox.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.post.dto.PostListResponse;
import io.teabag.assetbox.post.dto.PostResponse;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.util.TestUtil;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.post.service.PostService;
import io.teabag.assetbox.tag.dto.PopularTagResponse;
import io.teabag.assetbox.tag.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static io.teabag.assetbox.user.constants.Role.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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
    TagService tagService;

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

        private MockMultipartFile requestPart(PostCreateRequest request) throws Exception {
            return new MockMultipartFile(
                    "request",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(request)
            );
        }

        private MockMultipartFile assetPart(String originalName) {
            return new MockMultipartFile(
                    "assets",
                    originalName,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    originalName.getBytes()
            );
        }

        private UsernamePasswordAuthenticationToken currentUserAuthentication() {
            CurrentUser currentUser = CurrentUser.builder()
                    .id(1L)
                    .email("user@test.com")
                    .name("user")
                    .role(USER)
                    .build();

            return new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    currentUser.getAuthorities()
            );
        }


        @Test
        @DisplayName("201 Created와 성공 응답을 반환한다")
        void createPost() throws Exception {
            // given
            MockMultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "test image content".getBytes()
            );

            PostResponse response = new PostResponse(
                    1L,
                    "제목",
                    "내용",
                    1L,
                    1L,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null
            );

            given(postService.save(
                    any(CurrentUser.class),
                    any(PostCreateRequest.class),
                    any(MultipartFile.class),
                    anyList()
            )).willReturn(response);

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            multipart("/api/posts")
                                    .file(requestPart(request))
                                    .file(thumbnail)
                                    .file(assetPart("asset-1.png"))
                                    .file(assetPart("asset-2.png"))
                                    .with(csrf())
                                    .with(authentication(currentUserAuthentication()))
                    )

            //then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("제목"))
                    .andExpect(jsonPath("$.data.content").value("내용"))
                    .andExpect(jsonPath("$.data.authorId").value(1L))
                    .andExpect(jsonPath("$.data.thumbnailUrl").value("thumbnail-url"))
                    .andExpect(jsonPath("$.data.tags[0]").value("spring"))
            ;

            then(postService)
                    .should()
                    .save(
                            any(CurrentUser.class),
                            any(PostCreateRequest.class),
                            any(MultipartFile.class),
                            anyList()
                    );
        }
        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - title이 누락되면 400 VALIDATION_FAILED를 반환한다")
        void createPost_fail_when_title_is_blank() throws Exception {

            // given
            MockMultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "test image content".getBytes()
            );

            request = new PostCreateRequest(
                    "",
                    "내용",
                    1L,
                    List.of("spring", "jpa"),
                    null
            );

            PostResponse response = new PostResponse(
                    1L,
                    "제목",
                    "내용",
                    1L,
                    1L,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null
            );

            given(postService.save(
                    any(CurrentUser.class),
                    any(PostCreateRequest.class),
                    any(MultipartFile.class),
                    anyList()
            )).willReturn(response);

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            multipart("/api/posts")
                                    .file(requestPart(request))
                                    .file(thumbnail)
                                    .file(assetPart("asset-1.png"))
                                    .file(assetPart("asset-2.png"))
                                    .with(csrf())
                                    .with(authentication(currentUserAuthentication()))
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
            MockMultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "test image content".getBytes()
            );

            request = new PostCreateRequest(
                    "",
                    "내용",
                    1L,
                    List.of("spring", "jpa"),
                    null
            );

            PostResponse response = new PostResponse(
                    1L,
                    "제목",
                    "내용",
                    1L,
                    1L,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null
            );

            // when
            given(postService.save(
                    any(CurrentUser.class),
                    any(PostCreateRequest.class),
                    any(MultipartFile.class),
                    anyList()
            )).willReturn(response);

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                    multipart("/api/posts")
                            .file(requestPart(request))
                            .file(thumbnail)
                            .file(assetPart("asset-1.png"))
                            .file(assetPart("asset-2.png"))
                            .with(csrf())
                            .with(authentication(currentUserAuthentication()))
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
    @DisplayName("인기 태그 조회")
    class popular_tags_관련_테스트 {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/posts/popular-tags?limit=2 → 200 OK와 인기 태그 목록을 반환한다")
        void popularTags_success() throws Exception {
            // given
            PopularTagResponse tag1 = new PopularTagResponse("spring", 15L);
            PopularTagResponse tag2 = new PopularTagResponse("jpa", 10L);

            given(tagService.popularTags(2))
                    .willReturn(List.of(tag1, tag2));

            // when & then
            mockMvc.perform(
                            get("/api/posts/popular-tags")
                                    .param("limit", "2")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("spring"))
                    .andExpect(jsonPath("$.data[0].count").value(15))
                    .andExpect(jsonPath("$.data[1].name").value("jpa"))
                    .andExpect(jsonPath("$.data[1].count").value(10));

            then(tagService).should().popularTags(2);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/posts/popular-tags?limit=0 → 400 LIMIT_TOO_LARGE를 반환한다")
        void popularTags_fail_whenLimitIsZero() throws Exception {
            // given
            given(tagService.popularTags(0))
                    .willThrow(new BusinessException(ErrorCode.LIMIT_TOO_LARGE));

            // when & then
            mockMvc.perform(
                            get("/api/posts/popular-tags")
                                    .param("limit", "0")
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("LIMIT_TOO_LARGE"));
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

                PostResponse response = new PostResponse(
                        1L,
                        "제목",
                        "내용",
                        1L,
                        1L,
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        null
                );


                given(postService.getPost(postId))
                        .willReturn(response);

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

                List<PostResponse> items = List.of(
                        new PostResponse(
                                1L,
                                "제목",
                                "내용",
                                1L,
                                1L,
                                List.of(),
                                null,
                                null,
                                List.of(),
                                List.of(),
                                null
                        ),
                        new PostResponse(
                                2L,
                                "제목2",
                                "내용2",
                                2L,
                                1L,
                                List.of(),
                                null,
                                null,
                                List.of(),
                                List.of(),
                                null
                        )
                );

                PostListResponse response = new PostListResponse(
                        items,
                        0,
                        2,
                        true
                );

                given(postService.getPosts(any(Pageable.class)))
                        .willReturn(response);

                // when
                mockMvc.perform(
                                get("/api/posts")
                                        .param("page", "0")
                                        .param("size", "2")
                                        .param("sort", "createdAt,desc")
                        )
                //then
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