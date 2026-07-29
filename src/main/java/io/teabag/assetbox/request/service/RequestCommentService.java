package io.teabag.assetbox.request.service;

import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.request.domain.RequestComment;
import io.teabag.assetbox.request.dto.RequestCommentListResponse;
import io.teabag.assetbox.request.dto.RequestCommentResponse;
import io.teabag.assetbox.request.repository.RequestCommentRepository;
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
public class RequestCommentService {

    private final RequestCommentRepository requestCommentRepository;
    private final UserService userService;

    @Transactional
    public RequestCommentResponse save(CurrentUser currentUser, Long requestId, CommentCreateRequest request) {
        User user = userService.currentUserToUser(currentUser);
        RequestComment comment = RequestComment.builder()
                .requestId(requestId)
                .authorId(user.getId())
                .authorNickname(user.getNickname())
                .parentId(request.parentId())
                .content(request.content())
                .build();
        requestCommentRepository.save(comment);
        return RequestCommentResponse.from(comment);
    }

    // 삭제는 작성자 본인만(서비스 레이어 권한 검증 — CLAUDE.md 규칙).
    @Transactional
    public void delete(CurrentUser currentUser, Long commentId) {
        User user = userService.currentUserToUser(currentUser);
        RequestComment comment = requestCommentRepository.findByIdOrThrow(commentId);
        if (!comment.getAuthorId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 댓글만 삭제할 수 있습니다.");
        }
        comment.softDelete();
    }

    @Transactional(readOnly = true)
    public RequestCommentListResponse getComments(Long requestId, Pageable pageable) {
        Slice<RequestComment> slice =
                requestCommentRepository.findAllByRequestIdAndDeletedAtIsNull(requestId, pageable);
        return RequestCommentListResponse.from(slice);
    }
}
