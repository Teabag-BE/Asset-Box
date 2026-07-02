package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.*;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final S3FileStorageService s3FileStorageService;
    private final FileValidator fileValidator;
    private final S3FileKeyGenerator s3FileKeyGenerator;
    //todo : userRepository 의존성 지우기
    private final UserRepository userRepository;


    @Override
    public String uploadThumbnail(MultipartFile file, ThumbnailPurpose purpose, Long purposeId) {
        fileValidator.validateThumbnail(file);

        String s3Key = s3FileKeyGenerator.generateThumbnail(
                purpose,
                purposeId,
                file.getOriginalFilename()
        );
        String uploadedS3Key = s3FileStorageService.uploadWiths3key(file, s3Key);
        deleteOnRollback(uploadedS3Key);
        return uploadedS3Key;
    }

    @Override
    @Transactional
    public FileUploadResponse uploadFiles(List<MultipartFile> files,
                                          FilePurpose purpose,
                                          Long purposeId,
                                          UUID uploadBatchId,
                                          User uploadedBy) {
        validateUploadingFiles(files, purpose, purposeId);

        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            FileResponse uploadInfo = upload(files.get(i), purpose, purposeId, uploadBatchId, (long) i+1, uploadedBy);
            uploadInfos.add(uploadInfo);
        }
        return new FileUploadResponse(uploadInfos);
    }

    @Override
    public FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request) {
        validateUploadingFiles(files, request);

        List<FileResponse> uploadInfos = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            FileUploadInfo info = request.fileInfos().get(i);
            User uploadedBy = userRepository.findById(info.uploadedBy())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            FileResponse uploadInfo = upload(files.get(i), info.purpose(), info.purposeId(), info.uploadBatchId(), (long) i+1, uploadedBy);
            uploadInfos.add(uploadInfo);
        }
        return new FileUploadResponse(uploadInfos);
    }

    // 파일 미리보기 presignedUrl 조회
    @Override
    public String getShowPresignedUrl(String s3Key) {
        return s3FileStorageService.createShowPresignedUrl(s3Key);
    }

    @Override
    public List<String> getShowPresignedUrlsByPurpose(String filePurpose, Long purposeId) {
        FilePurpose purpose = FilePurpose.valueOf(filePurpose);
        List<String> presignedUrls = new ArrayList<>();
        List<String> s3Keys = fileRepository.findByPurposeAndPurposeId(purpose, purposeId)
                .stream()
                .map(f -> f.getS3Key())
                .toList();
        for (String s3Key : s3Keys) {
            presignedUrls.add(s3FileStorageService.createShowPresignedUrl(s3Key));
        }
        return presignedUrls;
    }

    @Override
    public String getDownloadPresignedUrl(Long fileId) {
        File file = fileRepository.findById(fileId).orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return s3FileStorageService.createDownloadPresignedUrl(file.getS3Key(), file.getOriginalName());
    }

    @Override
    public List<FileAttachmentResponse> getFileAttachmentsByPurpose(FilePurpose purpose, Long purposeId) {
        return fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(purpose, purposeId)
            .stream()
            .map(file -> FileAttachmentResponse.from(
                file,
                s3FileStorageService.createShowPresignedUrl(file.getS3Key())
            ))
            .toList();
    }

    @Override
    @Transactional
    public FileUploadResponse updateFiles(List<MultipartFile> files,
                                          FileUpdateRequest request,
                                          FilePurpose purpose,
                                          Long purposeId,
                                          UUID uploadBatchId,
                                          User uploadedBy) {
        //update와 delete가 기존 파일 수가 맞는지 확인
        Long existCount = fileRepository.countByPurposeAndPurposeId(purpose, purposeId);
        System.out.println("existCount = " + existCount);
        if (!existCount.equals((long) request.uRequest().size() + request.dFileIds().size())) throw new BusinessException(ErrorCode.NOT_ENOUGH_FILE);

        //update의 파일과 newfile들의 파일 총용량 검증
        List<Long> uFileIds = request.uRequest().stream().map(f -> f.fileId()).toList();
        List<File> existFiles = fileRepository.findAllByIdIn(uFileIds);
        Long totalSizes = existFiles.stream().map(File::getSizeBytes).reduce(0L, Long::sum);
        fileValidator.validateFilesTotalSize(totalSizes, files);

        //파일 저장
        createFiles(files, purpose, purposeId, uploadBatchId, uploadedBy, request.cFileSortOrders());

        //순서만 변경
        updateFiles(request.uRequest());

        //파일 삭제
        deleteFiles(request.dFileIds());

        fileRepository.flush();
        //전체적으로 파일 불러와서 순서맞는지 확인
        List<File> resultFiles = fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(purpose, purposeId);
        for (int i = 0; i < resultFiles.size(); i++) {
            if (!resultFiles.get(i).getUploadOrder().equals((long)i+1)) throw new BusinessException(ErrorCode.NOT_SEQUENTIAL_ORDER);
        }
        return new FileUploadResponse(resultFiles.stream().map(FileResponse::from).toList());
    }

    @Override
    @Transactional
    public FileUploadResponse updateFilesTest(List<MultipartFile> files, FileUpdateRequest request, FilePurpose purpose, Long purposeId, CurrentUser currentUser) {
        User uploadedBy = userRepository.findById(currentUser.getId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FileUploadResponse response = updateFiles(files, request, purpose, purposeId, UUID.randomUUID(), uploadedBy);
        return response;
    }

    //파일 수정의 저장
    private void createFiles(List<MultipartFile> files, FilePurpose purpose, Long purposeId, UUID uploadBatchId, User uploadedBy, List<Long> cFileSortOrders) {
        if (files.isEmpty()) return;
        // 파일 생성 정보와 이미지수가 같지 않으면 오류
        if (cFileSortOrders.size() != files.size()) throw new BusinessException(ErrorCode.NOT_ENOUGH_INFO);
        for (int i = 0; i < files.size(); i++) {
            upload(files.get(i), purpose, purposeId, uploadBatchId, cFileSortOrders.get(i) , uploadedBy);
        }
    }

    //파일 수정의 순서만 변경
    private void updateFiles(List<FileURequest> uRequests) {
        if (uRequests.isEmpty()) return;
        for (FileURequest uRequest : uRequests) {
            File file = fileRepository.findById(uRequest.fileId()).orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            file.updateSortOrder(uRequest.sortOrder());
        }
    }

    //파일 수정의 삭제
    private void deleteFiles(List<Long> dFileIds) {
        if (dFileIds.isEmpty()) return;
        List<File> deleteFiles = fileRepository.findAllByIdIn(dFileIds);
        deleteFiles.forEach(file -> file.setDeletedAt());
        s3FileStorageService.deleteAll(deleteFiles.stream().map(File::getS3Key).toList());
    }


    //파일 업로드
    private FileResponse upload(MultipartFile file,
                                FilePurpose purpose,
                                Long purposeId,
                                UUID uploadBatchId,
                                Long sortOrder,
                                User uploadedBy) {
        //파일 유효성 검사
        fileValidator.validate(file);

        //파일 원래 이름과 확장자 추출
        String originalName = file.getOriginalFilename();
        String extension = fileValidator.extractExtension(originalName);

        AssetFileType fileType = AssetFileType.fromFile(purpose, extension);

        //s3key 생성
        String s3Key = s3FileKeyGenerator.generate(
            purpose,
            purposeId,
            fileType,
            uploadBatchId,
            originalName
        );

        //s3에 파일 업로드
        s3FileStorageService.upload(file, s3Key);
        deleteOnRollback(s3Key);
        log.info("Uploaded file {} with extension {} :: {}", originalName, extension,  s3Key);

        //파일 메타데이터 생성
        File storedFile = File.builder()
            .originalName(originalName)
            .s3Key(s3Key)
            .extension(extension)
            .sizeBytes(file.getSize())
            .purpose(purpose)
            .purposeId(purposeId)
            .fileType(fileType)
            .uploadBatchId(uploadBatchId.toString())
            .uploadOrder(sortOrder)
            .uploadedBy(uploadedBy)
            .build();

        // DB에 파일 메타데이터 저장
        File savedFile;
        try {
            savedFile = fileRepository.save(storedFile);
        } catch (RuntimeException e) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                deleteUploadedFileQuietly(s3Key);
            }
            throw e;
        }

        // 파일 응답 형식 반환
        return FileResponse.from(savedFile);
    }



    private void validateUploadingFiles(
        List<MultipartFile> files,
        FilePurpose purpose,
        Long purposeId
    ) {
        files.forEach(fileValidator::validate);
        validateFilesTotalSize(files, purpose, purposeId);
    }

    private void validateUploadingFiles(
        List<MultipartFile> files,
        FileUploadRequest request
    ) {
        if (files.size() != request.fileInfos().size()) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_INFO);
        }

        files.forEach(fileValidator::validate);

        Map<FilePurposeAndId, List<MultipartFile>> filesByPurpose = IntStream.range(0, files.size())
            .boxed()
            .collect(Collectors.groupingBy(
                index -> new FilePurposeAndId(
                    request.fileInfos().get(index).purpose(),
                    request.fileInfos().get(index).purposeId()
                ),
                Collectors.mapping(files::get, Collectors.toList())
            ));

        filesByPurpose.forEach((purposeAndId, groupedFiles) ->
            validateFilesTotalSize(groupedFiles, purposeAndId.purpose(), purposeAndId.purposeId())
        );
    }

    /*
    현재 게시글의 용량을 가져와서, 새로 추가하는 파일의 용량을 비교한다.
     */
    private void validateFilesTotalSize(
        List<MultipartFile> files,
        FilePurpose purpose,
        Long purposeId
    ) {

        long currentTotalSizeBytes = fileRepository.sumSizeBytesByPurposeAndPurposeId(
            purpose,
            purposeId
        );

        fileValidator.validateFilesTotalSize(currentTotalSizeBytes, files);
    }

    private void deleteOnRollback(String s3Key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteUploadedFileQuietly(s3Key);
                }
            }
        });
    }

    private void deleteUploadedFileQuietly(String s3Key) {
        try {
            s3FileStorageService.delete(s3Key);
        } catch (RuntimeException deleteException) {
            log.warn("Failed to compensate uploaded S3 file. s3Key={}", s3Key, deleteException);
        }
    }

    private record FilePurposeAndId(FilePurpose purpose, Long purposeId) {
    }



}
