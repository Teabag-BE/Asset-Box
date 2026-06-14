package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileResponse;
import io.teabag.assetbox.file.dto.FileUploadInfo;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
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
        fileValidator.validateImageExtension(file);

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
                                          AssetFileType fileType,
                                          UUID uploadBatchId,
                                          User uploadedBy) {
        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            FileResponse uploadInfo = upload(files.get(i), purpose, purposeId, fileType, uploadBatchId, (long) i+1, uploadedBy);
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
        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            FileResponse uploadInfo = upload(files.get(i), purpose, purposeId, fileTypes.get(i), uploadBatchId, (long) i+1, uploadedBy);
            uploadInfos.add(uploadInfo);
        }
        return new FileUploadResponse(uploadInfos);
    }

    @Override
    public FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request) {
        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            FileUploadInfo info = request.fileInfos().get(i);
            User uploadedBy = userRepository.findById(info.uploadedBy())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            FileResponse uploadInfo = upload(files.get(i), info.purpose(), info.purposeId(), info.fileType(), info.uploadBatchId(), (long) i+1, uploadedBy);
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



    //파일 업로드
    private FileResponse upload(MultipartFile file,
                                FilePurpose purpose,
                                Long purposeId,
                                AssetFileType fileType,
                                UUID uploadBatchId,
                                Long sortOrder,
                                User uploadedBy) {
        //파일 유효성 검사
        fileValidator.validate(file);

        //파일 원래 이름과 확장자 추출
        String originalName = file.getOriginalFilename();
        String extension = fileValidator.extractExtension(originalName);

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




}
