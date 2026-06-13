package io.teabag.assetbox.post.service;

import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final TagService tagService;
    private final PostRepository postRepository;

    @Transactional
    public Post save(PostCreateRequest request) {

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(request.authorId())
                .categoryId(request.categoryId())
                .linkedRequestId(request.linkedRequestId())
                .build();

        tagService.findOrCreateAll(request.tags())
                .forEach(post::addTag);

        return postRepository.save(post);
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
    public Slice<Post> getPosts(Pageable pageable) {
        return postRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findByIdOrThrow(postId);
    }
}
