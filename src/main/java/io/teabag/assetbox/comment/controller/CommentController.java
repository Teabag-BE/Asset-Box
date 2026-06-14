package io.teabag.assetbox.comment.controller;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentListResponse;
import io.teabag.assetbox.comment.dto.CommentUpdateRequest;
import io.teabag.assetbox.comment.service.CommentService;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostListResponse;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    // 댓글 생성
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Comment> saveComment(
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Comment savedComment = commentService.save(request);
        return ApiResponse.created(savedComment, SuccessCode.COMMENT_CREATED.getSuccessMessage());
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);

        return ApiResponse.ok();
    }

    //댓글 수정
    @PutMapping("/{commentId}")
    public ApiResponse<Comment> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        Comment updatedComment = commentService.updateComment(commentId, request);

        return ApiResponse.ok(updatedComment, SuccessCode.POST_UPDATED.getSuccessMessage());
    }

    // 댓글 다건 조회
    @GetMapping
    public ApiResponse<CommentListResponse> getComments(
            Pageable pageable
    ) {
        Slice<Comment> comments = commentService.getComments(pageable);
        return ApiResponse.ok(CommentListResponse.from(comments),SuccessCode.COMMENT_READ.getSuccessMessage());
    }

    // 댓글 단건 조회
    @GetMapping("/{commentId}")
    public ApiResponse<Comment> getComment(
            @PathVariable Long commentId
    ) {
        return ApiResponse.ok(commentService.getComment(commentId),SuccessCode.COMMENT_READ_SINGLE.getSuccessMessage());
    }

    public ApiResponse<Void> list() { return ApiResponse.ok(""); }
}
