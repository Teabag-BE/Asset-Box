package io.teabag.assetbox.post.service;

import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.*;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collector;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final TagService tagService;
    private final PostRepository postRepository;
    private final FileService fileService;

    @Transactional
    public PostResponse save(PostCreateRequest request, Long authorId, MultipartFile thumbnail){

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(authorId)
                .categoryId(request.categoryId())
                .linkedRequestId(request.linkedRequestId())
                .build();

        tagService.findOrCreateAll(request.tags())
                .forEach(post::addTag);

        postRepository.save(post);
        String thumbnailKey = fileService.uploadThumbnail(thumbnail, ThumbnailPurpose.POST, post.getId());
        post.setThumbnailKey(thumbnailKey);
        return PostResponse.from(post, null);
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
            return info.setThumbnailUrl(fileService.getShowPresignedUrl(info.thumbnailKey()));
        });

        return PostListResponse.from(postInfos);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);
        String thumbnailUrl = fileService.getShowPresignedUrl(post.getThumbnailKey());
        return PostResponse.from(post, thumbnailUrl);
    }
}
