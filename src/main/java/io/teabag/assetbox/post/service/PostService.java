package io.teabag.assetbox.post.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostResponse;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final TagRepository tagRepository;
    private final PostRepository postRepository;

    @Transactional
    public Post save(PostCreateRequest request){

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(request.authorId())
                .categoryId(request.categoryId())
                .linkedRequestId(request.linkedRequestId())
                .build();

        for (String tagName : request.tags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));

            post.addTag(tag);
        }

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdOrThrow(postId);

        post.softDelete();
    }

    @Transactional
    public Post updatePost(Long postId, PostUpdateRequest request){
        Post post = postRepository.findByIdOrThrow(postId);

        post.update(
                request.title(),
                request.content(),
                request.categoryId()
        );

        post.clearTags();

        if (request.tags() != null) {
            for (String tagName : request.tags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName)));

                post.addTag(tag);
            }
        }

        return post;


    }

    @Transactional(readOnly = true)
    public List<Post> getPosts() {
        return postRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findByIdOrThrow(postId);
    }
}
