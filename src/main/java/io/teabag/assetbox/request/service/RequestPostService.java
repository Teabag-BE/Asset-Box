package io.teabag.assetbox.request.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileURequest;
import io.teabag.assetbox.file.dto.FileUpdateRequest;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.dto.ReferenceImageSyncRequest;
import io.teabag.assetbox.request.dto.ReferenceImageSyncRequest.ExistingImage;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.constants.Major;
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

        // thumbnail이 있을 때만 실행
        if (hasFile(thumbnail)) {
            String thumbnailKey = fileService.uploadThumbnail(thumbnail, ThumbnailPurpose.REFERENCE, savedRequestPost.getId());
            savedRequestPost.setThumbnailKey(thumbnailKey);
        }

        // referenceImages에서 실제 파일만 골라낸 리스트 만듦
        List<MultipartFile> uploadableReferenceImages = nonEmptyFiles(referenceImages);
        if (!uploadableReferenceImages.isEmpty()) { // 업로드한 reference 파일이 하나라도 있을때만 파일 업로드
            UUID uploadBatchId = UUID.randomUUID();

            fileService.uploadFiles(
                    uploadableReferenceImages,
                    FilePurpose.REQUEST_REFERENCE,
                    savedRequestPost.getId(),
                    uploadBatchId,
                    user
            );
        }

        List<FileAttachmentResponse> referenceAttachments =
            fileService.getFileAttachmentsByPurpose(
                FilePurpose.REQUEST_REFERENCE,
                savedRequestPost.getId()
            );

        // 썸네일 key가 있을 때만 presigned URL 생성, 없으면 null
        String thumbnailUrl = getThumbnailUrl(savedRequestPost);

        return RequestResponse.from(
            savedRequestPost,
            thumbnailUrl,
            referenceAttachments
        );
    }


    // TODO: 요청글 작업  시, Reference 이미지 넘겨야할 필요가 있다면 추가헤주기(from에 오버로딩 완료)

    // 요청글 수정 - REQUESTED 상태때만 수정 가능
    @Transactional
    public RequestResponse update(
            Long requestId,
            CurrentUser currentUser,
            RequestCreateRequest request,
            MultipartFile thumbnail,
            List<MultipartFile> referenceImages,
            ReferenceImageSyncRequest referenceSync
    ){
        User user = userService.currentUserToUser(currentUser);
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestId);

        PreConditions.validate(
                requestPost.getRequesterId().equals(user.getId()),
                ErrorCode.FORBIDDEN
        );

        PreConditions.validate(
                requestPost.getStatus() == RequestStatus.REQUESTED,
                ErrorCode.REQUEST_NOT_EDITABLE
        );

        requestPost.update(
                request.title(),
                request.content(),
                request.assetType(),
                request.preferredStyle(),
                request.engine(),
                request.deadline()
        );

        List<MultipartFile> uploadableReferenceImages = nonEmptyFiles(referenceImages);
        PreConditions.validate(
                referenceSync != null || uploadableReferenceImages.isEmpty(),
                ErrorCode.NOT_ENOUGH_INFO
        );

        FileUpdateRequest fileUpdateRequest = null;
        if (referenceSync != null) {
            fileUpdateRequest = createFileUpdateRequest(
                    requestPost.getId(),
                    uploadableReferenceImages,
                    referenceSync
            );
        }

        String previousThumbnailKey = null;
        if (hasFile(thumbnail)) {
            previousThumbnailKey = requestPost.getThumbnailKey();
            String thumbnailKey = fileService.uploadThumbnail(
                    thumbnail,
                    ThumbnailPurpose.REFERENCE,
                    requestPost.getId()
            );
            requestPost.setThumbnailKey(thumbnailKey);
        }

        if (fileUpdateRequest != null) {
            // #186에서 updateFiles → updateReferenceFiles 로 리네임됨 (시그니처 동일)
            fileService.updateReferenceFiles(
                    uploadableReferenceImages,
                    fileUpdateRequest,
                    FilePurpose.REQUEST_REFERENCE,
                    requestPost.getId(),
                    UUID.randomUUID(),
                    user
            );
        }

        List<FileAttachmentResponse> referenceAttachments =
                fileService.getFileAttachmentsByPurpose(
                        FilePurpose.REQUEST_REFERENCE,
                        requestPost.getId()
                );

        // 구 썸네일 삭제는 예외가 날 수 있는 모든 단계(파일 동기화 등)를 지난 마지막에 수행한다.
        // 먼저 지우면 이후 예외로 DB 가 옛 키로 롤백될 때 그 키의 객체가 이미 없어 썸네일이 영구 깨진다.
        if (previousThumbnailKey != null) {
            fileService.deleteStorageObject(previousThumbnailKey);
        }

        return RequestResponse.from(
                requestPost,
                getThumbnailUrl(requestPost),
                referenceAttachments
        );
    }

    private FileUpdateRequest createFileUpdateRequest(
            Long requestId,
            List<MultipartFile> newFiles,
            ReferenceImageSyncRequest referenceSync
    ) {
        PreConditions.validate(
                newFiles.size() == referenceSync.newFileSortOrders().size(),
                ErrorCode.NOT_ENOUGH_INFO
        );

        Set<Long> currentFileIds = fileService.getFileAttachmentsByPurpose(
                        FilePurpose.REQUEST_REFERENCE,
                        requestId
                )
                .stream()
                .map(FileAttachmentResponse::fileId)
                .collect(java.util.stream.Collectors.toSet());

        List<Long> keepFileIds = referenceSync.existingImages().stream()
                .map(ExistingImage::fileId)
                .toList();
        Set<Long> keepFileIdSet = new HashSet<>(keepFileIds);

        PreConditions.validate(
                keepFileIds.size() == keepFileIdSet.size(),
                ErrorCode.INPUT_NOT_VALID
        );
        PreConditions.validate(
                currentFileIds.containsAll(keepFileIdSet),
                ErrorCode.FILE_NOT_FOUND
        );

        List<Long> actualSortOrders = Stream.concat(
                referenceSync.existingImages().stream().map(ExistingImage::sortOrder),
                referenceSync.newFileSortOrders().stream()
        ).sorted().toList();
        List<Long> expectedSortOrders = LongStream.rangeClosed(1, actualSortOrders.size())
                .boxed()
                .toList();

        PreConditions.validate(
                actualSortOrders.equals(expectedSortOrders),
                ErrorCode.NOT_SEQUENTIAL_ORDER
        );

        List<Long> deleteFileIds = currentFileIds.stream()
                .filter(fileId -> !keepFileIdSet.contains(fileId))
                .toList();

        List<FileURequest> updateFileRequests = referenceSync.existingImages().stream()
                .map(existingImage -> new FileURequest(
                        existingImage.fileId(),
                        existingImage.sortOrder()
                ))
                .toList();

        return new FileUpdateRequest(
                referenceSync.newFileSortOrders(),
                updateFileRequests,
                deleteFileIds
        );
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
    public RequestResponse assign(Long requestId, CurrentUser currentUser) {
        PreConditions.validate(
                currentUser.getMajor() == Major.TA,
                ErrorCode.REQUEST_ASSIGN_FORBIDDEN
        );

        Long assigneeId = currentUser.getId();
        RequestPost requestPost = requestPostRepository.findByIdOrThrow(requestId);

        PreConditions.validate(
                requestPost.getStatus() != RequestStatus.COMPLETED,
                ErrorCode.REQUEST_COMPLETED_LOCKED
        );

        PreConditions.validate(
                !assigneeId.equals(requestPost.getRequesterId()),
                ErrorCode.REQUEST_ASSIGN_REQUESTER_NOT_ALLOWED
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

    // 내용에 있는 파일만 true
    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    // 업로드 가능한 파일만 추림.
    private List<MultipartFile> nonEmptyFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of(); // 리스트 자체가 없거나 비어있으면 빈 리스트 반환
        }
        // 리스트 안의 빈 파일 파트를 제거하고 실제 파일만 남김
        return files.stream()
                .filter(this::hasFile)
                .toList();
    }
}
