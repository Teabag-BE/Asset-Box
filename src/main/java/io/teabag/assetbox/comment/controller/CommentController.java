package io.teabag.assetbox.comment.controller;

import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.service.CommentService;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ApiResponse<Void> list() { return ApiResponse.ok(""); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CommentResponse response = commentService.createComment(postId, currentUser.getId(), request);
        return ApiResponse.created(response, SuccessCode.COMMENT_CREATED.getSuccessMessage());
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        commentService.deleteComment(postId, commentId, currentUser.getId());
        return ApiResponse.ok(SuccessCode.COMMENT_DELETED.getSuccessMessage());
    }
}