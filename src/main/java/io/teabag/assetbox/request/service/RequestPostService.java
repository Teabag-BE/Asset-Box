package io.teabag.assetbox.request.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestPostService {

    private final RequestPostRepository requestPostRepository;
    private final FileService fileService;

    @Transactional
    public RequestResponse save(RequestCreateRequest request, Long requesterId, MultipartFile thumbnail) {
        RequestPost requestPost = createRequestPost(request, requesterId);

        requestPostRepository.save(requestPost);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String thumbnailKey = fileService.uploadThumbnail(
                    thumbnail,
                    ThumbnailPurpose.REFERENCE,
                    requestPost.getId()
            );
            requestPost.setThumbnailKey(thumbnailKey);
        }

        return RequestResponse.from(requestPost, null);
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

        return RequestResponse.from(requestPost, getThumbnailUrl(requestPost));
    }

    // 요청 수락
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

        return RequestResponse.from(requestPost, getThumbnailUrl(requestPost));
    }

    // 요청글 다건 조회
    @Transactional(readOnly = true)
    public RequestListResponse getRequests(Pageable pageable) {
        Slice<RequestResponse> requestPosts = requestPostRepository.findAllByDeletedAtIsNull(pageable)
                .map(requestPost -> RequestResponse.from(requestPost, getThumbnailUrl(requestPost)));

        return RequestListResponse.fromResponses(requestPosts);
    }

    // 요청글 단건 조회
    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long requestId) {
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestId);
        return RequestResponse.from(requestPost, getThumbnailUrl(requestPost));
    }





    private RequestPost createRequestPost(RequestCreateRequest request, Long requesterId) {
        return RequestPost.builder()
                .title(request.title())
                .content(request.content())
                .assetType(request.assetType())
                .preferredStyle(request.preferredStyle())
                .engine(request.engine())
                .deadline(request.deadline())
                .requesterId(requesterId)
                .build();
    }

    private String getThumbnailUrl(RequestPost requestPost) {
        if (requestPost.getThumbnailKey() == null) {
            return null;
        }
        return fileService.getShowPresignedUrl(requestPost.getThumbnailKey());
    }
}
