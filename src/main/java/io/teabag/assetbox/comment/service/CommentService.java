package io.teabag.assetbox.comment.service;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentUpdateRequest;
import io.teabag.assetbox.comment.repository.CommentRepository;
import io.teabag.assetbox.post.domain.Post;

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
    public Comment save(CommentCreateRequest request){

        Comment comment = Comment.builder()
                .content(request.content())
                .parentId(request.parentId())
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long postId) {
        Comment comment = commentRepository.findByIdOrThrow(postId);

        comment.softDelete();
    }

    @Transactional
    public Comment updateComment(Long commentId, CommentUpdateRequest request){
        Comment comment = commentRepository.findByIdOrThrow(commentId);

        comment.update(
                request.content()
        );

        return comment;
    }

    @Transactional(readOnly = true)
    public Slice<Comment> getComments(Pageable pageable) {
        return commentRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Comment getComment(Long commentId) {
        return commentRepository.findByIdOrThrow(commentId);
    }
}
