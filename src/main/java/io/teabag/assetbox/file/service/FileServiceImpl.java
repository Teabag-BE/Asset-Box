package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.*;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        return s3FileStorageService.uploadWiths3key(file, s3Key);
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
    @Transactional
    public FileUploadResponse uploadFiles(List<MultipartFile> files,
                                          FilePurpose purpose,
                                          Long purposeId,
                                          List<AssetFileType> fileTypes,
                                          UUID uploadBatchId,
                                          User uploadedBy) {
        if (files.size() != fileTypes.size()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        validateUploadingFiles(files, purpose, purposeId);

        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            FileResponse uploadInfo = upload(files.get(i), purpose, purposeId, fileTypes.get(i), uploadBatchId, (long) i+1, uploadedBy);
            uploadInfos.add(uploadInfo);
        }
        return new FileUploadResponse(uploadInfos);
    }

    @Override
    public FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request) {
        // validatePostTotalSize(files, request.fileInfos()., purposeId);

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

    //todo : purpose와 purposeId가 안맞아 파일을 못찾는 경우 나중에 생각하기
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
        return fileRepository.findByPurposeAndPurposeIdOrderByUploadOrderAsc(purpose, purposeId)
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
        //파일 저장
        createFiles(files, purpose, purposeId, uploadBatchId, uploadedBy, request.cFileSortOrders());

        //순서만 변경
        updateFiles(request.uRequest());

        //파일 삭제
        deleteFiles(request.dFileIds());

        //전체적으로 파일 불러와서 순서맞는지 확인
        List<File> resultFiles = fileRepository.findByPurposeAndPurposeIdOrderByUploadOrderAsc(purpose, purposeId);
        for (int i = 0; i < resultFiles.size(); i++) {
            if (!resultFiles.get(i).getUploadOrder().equals(i+1)) throw new BusinessException(ErrorCode.NOT_SEQUENTIAL_ORDER);
        }
        return new FileUploadResponse(resultFiles.stream().map(FileResponse::from).toList());
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
        File savedFile = fileRepository.save(storedFile);

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

    //TODO:FileUploadrequest로 게시물당 20MB 넘는지 검증하는 로직 미구현
    private void validateUploadingFiles(
        List<MultipartFile> files,
        FileUploadRequest request
    ) {

        files.forEach(fileValidator::validate);

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



}
