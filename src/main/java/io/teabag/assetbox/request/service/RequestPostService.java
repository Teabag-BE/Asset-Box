package io.teabag.assetbox.request.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestPostService {

    private final RequestPostRepository requestPostRepository;

    @Transactional
    public RequestPost save(RequestCreateRequest request) {
        RequestPost requestPost = RequestPost.builder()
                .title(request.title())
                .content(request.content())
                .assetType(request.assetType())
                .preferredStyle(request.preferredStyle())
                .engine(request.engine())
                .deadline(request.deadline())
                .requesterId(request.requesterId())
                .build();

        return requestPostRepository.save(requestPost);
    }

    // 요청글 삭제 - REQUESTED 상태때만 삭제 가능
    @Transactional
    public void deleteRequestPost(Long requestPostId) {
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestPostId);

        PreConditions.validate(
        requestPost.getStatus() == RequestStatus.REQUESTED,
        ErrorCode.REQUEST_NOT_DELETABLE
        );

        requestPost.softDelete();
    }

    // 요청글 완료 - IN_PROGRESS 상태때만 완료 가능
    @Transactional
    public RequestResponse completeByLinkedPost(Long requestId, Long assigneeId, Long linkedPostId) {
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestId);

        PreConditions.validate(
                requestPost.getAssigneeId().equals(assigneeId),
                ErrorCode.REQUEST_ASSIGNEE_MISMATCH
        );

        PreConditions.validate(
                requestPost.getStatus() == RequestStatus.IN_PROGRESS,
                ErrorCode.POST_LINKED_REQUEST_INVALID_STATUS
        );

        PreConditions.validate(
                requestPost.getLinkedPostId() == null,
                ErrorCode.REQUEST_ALREADY_COMPLETED
        );

        requestPost.complete(linkedPostId);

        // TODO: 시스템 DM 발송
        // "요청이 완료되었습니다 → /posts/{linkedPostId}"

        return RequestResponse.from(requestPost);
    }

    @Transactional
    public RequestResponse assign(Long requestId, Long assigneeId) {
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestId);

        PreConditions.validate(
                requestPost.getStatus() != RequestStatus.COMPLETED,
                ErrorCode.REQUEST_COMPLETED_LOCKED
        );

        PreConditions.validate(
                !assigneeId.equals(requestPost.getAssigneeId()),
                ErrorCode.REQUEST_ASSIGN_SELF_DUPLICATED
        );

        PreConditions.validate(
                requestPost.getAssigneeId() == null,
                ErrorCode.REQUEST_ASSIGN_TAKEN
        );

        PreConditions.validate(
                requestPost.getStatus() == RequestStatus.REQUESTED,
                ErrorCode.REQUEST_NOT_ASSIGNABLE
        );

        requestPost.assign(assigneeId);

        // TODO: 시스템 DM 발송
        // "{assigneeNickname}님이 요청을 수락했습니다."

        return RequestResponse.from(requestPost);
    }







    // 요청글 다건 조회
    @Transactional(readOnly = true)
    public Slice<RequestPost> getRequests(Pageable pageable) {
        return requestPostRepository.findAllByDeletedAtIsNull(pageable);
    }

    // 요청글 단건 조회
    @Transactional(readOnly = true)
    public RequestPost getRequest(Long requestId) {
        return requestPostRepository.findByIdOrThrow(requestId);
    }
}
