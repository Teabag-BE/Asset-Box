package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.request.dto.RequestCommentListResponse;
import io.teabag.assetbox.request.dto.RequestCommentResponse;
import io.teabag.assetbox.request.service.RequestCommentService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests/{requestId}/comments")
public class RequestCommentController {

    private final RequestCommentService requestCommentService;

    // 요청 댓글 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RequestCommentResponse> saveComment(
            @PathVariable Long requestId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        RequestCommentResponse saved = requestCommentService.save(currentUser, requestId, request);
        return ApiResponse.created(saved, SuccessCode.COMMENT_CREATED.getSuccessMessage());
    }

    // 요청 댓글 삭제(작성자 본인만)
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        requestCommentService.delete(currentUser, commentId);
        return ApiResponse.ok(SuccessCode.COMMENT_DELETED.getSuccessMessage());
    }

    // 요청 댓글 다건 조회
    @GetMapping
    public ApiResponse<RequestCommentListResponse> getComments(
            @PathVariable Long requestId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        RequestCommentListResponse comments = requestCommentService.getComments(requestId, pageable);
        return ApiResponse.ok(comments, SuccessCode.COMMENT_READ.getSuccessMessage());
    }
}
