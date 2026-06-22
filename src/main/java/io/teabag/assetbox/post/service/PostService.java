package io.teabag.assetbox.post.service;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.AssetFileRequest;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.file.service.FileValidator;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.*;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.tag.service.TagService;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final TagService tagService;
    private final PostRepository postRepository;
    private final FileService fileService;
    private final UserService userService;
    private final RequestPostService requestPostService;

    @Transactional
    public PostResponse save(CurrentUser currentUser, PostCreateRequest request, MultipartFile thumbnail, List<MultipartFile> assets) {
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

        //썸네일 저장
        String thumbnailKey = fileService.uploadThumbnail(thumbnail, ThumbnailPurpose.POST, post.getId());
        post.setThumbnailKey(thumbnailKey);

        //썸네일 url 불러오기
        String thumbnailUrl = fileService.getShowPresignedUrl(thumbnailKey);

        //파일 저장
        UUID batchedId = UUID.randomUUID();
        FileUploadResponse fileUploadResponse = fileService.uploadFiles(assets, FilePurpose.ASSET, post.getId(), batchedId, user);
        //응답 반환
        return PostResponse.from(post, thumbnailUrl, fileUploadResponse);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);

        post.softDelete();
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
            info.setThumbnailUrl(fileService.getShowPresignedUrl(info.thumbnailKey()));
            List<FileAttachmentResponse> files = fileService.getFileAttachmentsByPurpose(FilePurpose.ASSET, post.getId());
            List<PostFileInfo> fileList = files.stream().map(PostFileInfo::from).toList();
            info = info.setfiles(fileList);
            return info;
        });

        return PostListResponse.from(postInfos);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);
        String thumbnailUrl = fileService.getShowPresignedUrl(post.getThumbnailKey());
        List<FileAttachmentResponse> fileResponse = fileService.getFileAttachmentsByPurpose(FilePurpose.ASSET, post.getId());
        return PostResponse.from(post, thumbnailUrl,fileResponse);
    }
}
