package io.teabag.assetbox.post.controller;

import java.util.List;

import io.teabag.assetbox.post.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.service.PostService;
import io.teabag.assetbox.tag.dto.PopularTagResponse;
import io.teabag.assetbox.tag.service.TagService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final TagService tagService;

    // 게시물 생성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> savePost(
            @Valid @RequestPart("request") PostCreateRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("assets") List<MultipartFile> assets,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {
        PostResponse savedPost = postService.save(currentUser, request, thumbnail, assets);
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

    @GetMapping("/popular-tags")
    public ApiResponse<List<PopularTagResponse>> popularTags(
            @RequestParam(required = false) Integer limit
    ) {
        List<PopularTagResponse> popularTags = tagService.popularTags(limit);
        return ApiResponse.ok(popularTags, SuccessCode.POPULAR_TAG_READ.getSuccessMessage());
    }

    // 게시물 다건 조회
    @GetMapping
    public ApiResponse<PostListResponse> getPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PostListResponse posts = postService.getPosts(pageable);

        return ApiResponse.ok(posts,SuccessCode.POST_READ.getSuccessMessage());
    }

    // 게시글 단건 조회
    @GetMapping("/{postId}")
    public ApiResponse<PostReadResponse> getPost(
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(postService.getPost(postId),SuccessCode.POST_READ_SINGLE.getSuccessMessage());
    }
}
