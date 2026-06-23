package io.teabag.assetbox.request.service;

import java.util.List;
import java.util.UUID;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadInfo;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
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
    private final UserService userService;

    @Transactional
    public RequestResponse save(
        CurrentUser currentUser,
        RequestCreateRequest request,
        MultipartFile thumbnail,
        List<MultipartFile> referenceImages
    ) {
        User user = userService.currentUserToUser(currentUser);
        RequestPost requestPost = createRequestPost(request, user.getId());

        RequestPost savedRequestPost = requestPostRepository.save(requestPost);

        String thumbnailKey = fileService.uploadThumbnail(thumbnail, ThumbnailPurpose.REFERENCE, savedRequestPost.getId());
        savedRequestPost.setThumbnailKey(thumbnailKey);

        if (referenceImages != null && !referenceImages.isEmpty()) {
            UUID uploadBatchId = UUID.randomUUID();

            fileService.uploadFiles(
                referenceImages,FilePurpose.REQUEST_REFERENCE, savedRequestPost.getId(),
                    AssetFileType.REFERENCE, uploadBatchId, user
            );
        }

        List<FileAttachmentResponse> referenceAttachments =
            fileService.getFileAttachmentsByPurpose(
                FilePurpose.REQUEST_REFERENCE,
                savedRequestPost.getId()
            );

        String thumbnailUrl = fileService.getShowPresignedUrl(thumbnailKey);

        return RequestResponse.from(
            savedRequestPost,
            thumbnailUrl,
            referenceAttachments
        );
    }


    // TODO: 요청글 작업  시, Reference 이미지 넘겨야할 필요가 있다면 추가헤주기(from에 오버로딩 완료)

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

        List<FileAttachmentResponse> referenceAttachments =
            fileService.getFileAttachmentsByPurpose(
                FilePurpose.REQUEST_REFERENCE,
                requestPost.getId()
            );

        String thumbnailUrl = getThumbnailUrl(requestPost);

        return RequestResponse.from(
            requestPost,
            thumbnailUrl,
            referenceAttachments
        );
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
