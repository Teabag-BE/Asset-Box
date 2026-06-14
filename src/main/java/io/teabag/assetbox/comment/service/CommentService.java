package io.teabag.assetbox.comment.service;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentListResponse;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.dto.CommentUpdateRequest;
import io.teabag.assetbox.comment.repository.CommentRepository;
import io.teabag.assetbox.post.domain.Post;

import io.teabag.assetbox.post.dto.PostInfo;
import io.teabag.assetbox.post.dto.PostListResponse;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.repository.TagRepository;
import io.teabag.assetbox.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse save(CommentCreateRequest request){

        Comment comment = Comment.builder()
                .content(request.content())
                .parentId(request.parentId())
                .build();

        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long postId) {
        Comment comment = commentRepository.findByIdOrThrow(postId);

        comment.softDelete();
    }

    @Transactional(readOnly = true)
    public CommentListResponse getComments(Pageable pageable) {
        Slice<Comment> comments = commentRepository.findAllByDeletedAtIsNull(pageable);

        Slice<CommentInfo> commentInfos = comments.map(comment -> {
            return PostInfo.from(comment);
        });
        return CommentListResponse.from(commentInfos);
    }
}
