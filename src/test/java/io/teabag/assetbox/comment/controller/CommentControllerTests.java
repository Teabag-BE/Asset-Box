package io.teabag.assetbox.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.service.CommentService;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTests {

    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken currentUserAuthentication() {
        CurrentUser currentUser = CurrentUser.builder()
                .id(10L)
                .email("user@test.com")
                .name("user")
                .role(Role.USER)
                .build();

        return new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                currentUser.getAuthorities()
        );
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {
        CommentCreateRequest request;

        @BeforeEach
        void setUp() {
            request = new CommentCreateRequest("댓글 내용", null);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("201 Created와 성공 응답을 반환한다")
        void createComment() throws Exception {
            // given
            CommentResponse response = new CommentResponse(
                    1L, 1L, 10L, null, "댓글 내용", false
            );
            given(commentService.createComment(eq(1L), anyLong(), any(CommentCreateRequest.class)))
                    .willReturn(response);

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                    post("/api/posts/{postId}/comments", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            )
            // then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("댓글 내용"))
                    .andExpect(jsonPath("$.data.postId").value(1))
                    .andExpect(jsonPath("$.data.deleted").value(false));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - content가 누락되면 400 VALIDATION_FAILED를 반환한다")
        void createComment_fail_when_content_is_blank() throws Exception {
            // given
            request = new CommentCreateRequest("", null);

            // when
            mockMvc.perform(
                    post("/api/posts/{postId}/comments", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            )
            // then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.toString()))
                    .andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getDescription()));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("댓글 삭제 요청 시 200 OK와 성공 응답을 반환한다")
        void deleteComment_success() throws Exception {
            // given
            willDoNothing()
                    .given(commentService)
                    .deleteComment(1L, 100L, 10L);

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            delete("/api/posts/{postId}/comments/{commentId}", 1L, 100L)
                                    .with(csrf())
                    )
            // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(commentService)
                    .should()
                    .deleteComment(1L, 100L, 10L);
        }
    }
}
