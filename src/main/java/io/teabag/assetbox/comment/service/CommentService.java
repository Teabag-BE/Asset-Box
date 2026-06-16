package io.teabag.assetbox.comment.service;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.*;
import io.teabag.assetbox.comment.repository.CommentRepository;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService;

    @Transactional
    public CommentResponse save(CurrentUser currentUser, Long postId, CommentCreateRequest request){
        User user = userService.currenUserToUser(currentUser);
        Comment comment = Comment.builder()
                .postId(postId)
                .content(request.content())
                .authorId(user.getId())
                .authorNickname(user.getNickname())
                .parentId(request.parentId())
                .build();

        commentRepository.save(comment);

        return CommentResponse.from(comment, user);
    }

    @Transactional
    public void deleteComment(Long postId) {
        Comment comment = commentRepository.findByIdOrThrow(postId);

        comment.softDelete();
    }

    @Transactional(readOnly = true)
    public CommentListResponse getComments(Long postId, Pageable pageable) {
        Slice<CommentInfo> commentInfos =
                commentRepository.findAllByPostIdAndDeletedAtIsNull(postId, pageable)
                        .map(CommentInfo::from);

        return CommentListResponse.from(commentInfos);
    }
}
