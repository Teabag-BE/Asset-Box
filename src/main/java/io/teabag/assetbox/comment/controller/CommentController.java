package io.teabag.assetbox.comment.controller;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentListResponse;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.service.CommentService;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    // 댓글 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> saveComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CommentResponse savedComment = commentService.save(currentUser, postId, request);
        return ApiResponse.created(savedComment, SuccessCode.COMMENT_CREATED.getSuccessMessage());
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);

        return ApiResponse.ok(SuccessCode.COMMENT_DELETED.getSuccessMessage());
    }


    // 댓글 다건 조회
    @GetMapping
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        CommentListResponse comments = commentService.getComments(postId, pageable);
        return ApiResponse.ok(comments, SuccessCode.COMMENT_READ.getSuccessMessage());
    }
}
