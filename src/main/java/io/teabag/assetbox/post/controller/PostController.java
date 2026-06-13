package io.teabag.assetbox.post.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.constants.SuccessCode;
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
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 게시물 생성
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Post> savePost(
            @Valid @RequestBody PostCreateRequest request
    ) {
        Post savedPost = postService.save(request);
        return ApiResponse.created(savedPost, SuccessCode.POST_CREATED.getSuccessMessage());
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable Long postId
    ) {
        postService.deletePost(postId);

        return ApiResponse.ok(SuccessCode.POST_DELETED.getSuccessMessage());
    }

    //게시물 수정
    @PutMapping("/{postId}")
    public ApiResponse<Post> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        Post updatedPost = postService.updatePost(postId, request);

        return ApiResponse.ok(updatedPost, SuccessCode.POST_UPDATED.getSuccessMessage());
    }

    // 게시물 다건 조회
    @GetMapping
    public ApiResponse<PostListResponse> getPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Slice<Post> posts = postService.getPosts(pageable);

        return ApiResponse.ok(PostListResponse.from(posts),SuccessCode.POST_READ.getSuccessMessage());
    }

    // 게시글 단건 조회
    @GetMapping("/{postId}")
    public ApiResponse<Post> getPost(
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(postService.getPost(postId),SuccessCode.POST_READ_SINGLE.getSuccessMessage());
    }
}
