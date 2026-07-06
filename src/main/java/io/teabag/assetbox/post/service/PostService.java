package io.teabag.assetbox.post.service;

import java.util.List;
import java.util.UUID;

import io.teabag.assetbox.post.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.tag.service.TagService;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private static final Duration STORAGE_RETENTION = Duration.ofDays(7);

    private final TagService tagService;
    private final PostRepository postRepository;
    private final FileService fileService;
    private final UserService userService;
    private final RequestPostService requestPostService;

    @Transactional
    public PostResponse save(CurrentUser currentUser, PostCreateRequest request, MultipartFile thumbnail, MultipartFile assetZip) {
        User user = userService.currentUserToUser(currentUser);
        //포스트 저장
        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(user.getId())
                .categoryId(request.categoryId())
                .linkedRequestId(request.linkedRequestId())
                .build();
        tagService.findOrCreateAll(request.tags())
                .forEach(post::addTag);
        Post savedPost = postRepository.save(post);

        // request post 자동 completed
        if(request.linkedRequestId() != null){
            requestPostService.completeByLinkedPost(
                    request.linkedRequestId(),
                    user.getId(),
                    savedPost.getId()
            );
        }

        String thumbnailKey = null;
        try {
            //썸네일 저장
            thumbnailKey = fileService.uploadThumbnail(thumbnail, ThumbnailPurpose.POST, post.getId());
            post.setThumbnailKey(thumbnailKey);

            //썸네일 url 불러오기
            String thumbnailUrl = fileService.getShowPresignedUrl(thumbnailKey);

            //파일 저장
            UUID batchedId = UUID.randomUUID();
            FileUploadResponse fileUploadResponse = fileService.uploadFiles(List.of(assetZip), FilePurpose.ASSET, post.getId(), batchedId, user);
            //응답 반환
            return PostResponse.from(post, thumbnailUrl, fileUploadResponse);
        } catch (RuntimeException e) {
            deleteUploadedThumbnail(thumbnailKey);
            throw e;
        }
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);

        post.softDelete();
        post.markThumbnailDeletedWithRetention(STORAGE_RETENTION);
        fileService.deleteFilesByPurpose(FilePurpose.ASSET, postId);
    }

    @Transactional
    public Post updatePost(Long postId, PostUpdateRequest request) {
        Post post = postRepository.findByIdOrThrow(postId);

        post.update(
                request.title(),
                request.content(),
                request.categoryId()
        );

        post.clearTags();
        tagService.findOrCreateAll(request.tags())
                .forEach(post::addTag);

        return post;
    }

    @Transactional(readOnly = true)
    public PostListResponse getPosts(Pageable pageable) {
        Slice<Post> posts = postRepository.findAllByDeletedAtIsNull(pageable);

        Slice<PostInfo> postInfos = posts.map(post -> {
            PostInfo info = PostInfo.from(post);
            info = info.setThumbnailUrl(fileService.getShowPresignedUrl(info.thumbnailKey()));
            List<FileAttachmentResponse> files = fileService.getFileAttachmentsByPurposeAndFileType(
                    FilePurpose.ASSET,
                    post.getId(),
                    AssetFileType.ZIP
            );
            List<PostFileInfo> fileList = files.stream().map(PostFileInfo::from).toList();
            info = info.setfiles(fileList);
            return info;
        });

        return PostListResponse.from(postInfos);
    }

    @Transactional(readOnly = true)
    public PostReadResponse getPost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);
        String thumbnailUrl = fileService.getShowPresignedUrl(post.getThumbnailKey());
        List<FileAttachmentResponse> fileResponse = fileService.getFileAttachmentsByPurposeAndFileType(
                FilePurpose.ASSET,
                post.getId(),
                AssetFileType.ZIP
        );
        PostDownloadFileResponse downloadFile = fileResponse.stream()
                .findFirst()
                .map(PostDownloadFileResponse::from)
                .orElse(null);
        PostViewerResponse viewer = buildViewerOrNull(post.getId());
        return PostReadResponse.from(post, thumbnailUrl, fileResponse, downloadFile, viewer);
    }

    @Transactional(readOnly = true)
    public PostViewerResponse getPostViewer(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);
        return buildViewerOrThrow(post.getId());
    }

    private PostViewerResponse buildViewerOrNull(Long postId) {
        return buildViewer(postId, false);
    }

    private PostViewerResponse buildViewerOrThrow(Long postId) {
        return buildViewer(postId, true);
    }

    private PostViewerResponse buildViewer(Long postId, boolean modelRequired) {
        List<FileAttachmentResponse> modelFiles = fileService.getFileAttachmentsByPurposeAndFileType(
                FilePurpose.ASSET,
                postId,
                AssetFileType.MODEL
        );

        if (modelFiles.isEmpty()) {
            if (!modelRequired) {
                return null;
            }
            throw new BusinessException(ErrorCode.VIEWER_MODEL_NOT_FOUND);
        }

        if (modelFiles.size() > 1) {
            throw new BusinessException(ErrorCode.VIEWER_MODEL_COUNT_INVALID);
        }

        List<PostViewerFileResponse> textures = fileService.getFileAttachmentsByPurposeAndFileType(
                FilePurpose.ASSET,
                postId,
                AssetFileType.TEXTURE
            )
            .stream()
            .map(PostViewerFileResponse::from)
            .toList();

        return new PostViewerResponse(
            postId,
            PostViewerFileResponse.from(modelFiles.getFirst()),
            textures
        );
    }

    private void deleteUploadedThumbnail(String thumbnailKey) {
        if (thumbnailKey == null) {
            return;
        }

        try {
            fileService.deleteStorageObject(thumbnailKey);
        } catch (Exception deleteException) {
            log.warn("Failed to compensate uploaded thumbnail. thumbnailKey = {}", thumbnailKey, deleteException);
        }
    }
}
