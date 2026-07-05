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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Duration STORAGE_RETENTION = Duration.ofDays(7);

    private final FileRepository fileRepository;
    private final S3FileStorageService s3FileStorageService;
    private final FileValidator fileValidator;
    private final S3FileKeyGenerator s3FileKeyGenerator;
    private final ZipExtractService zipExtractService;
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
        if (purpose == FilePurpose.ASSET) {
            validateSingleAssetZip(files);
            return uploadAssetZip(files.getFirst(), purposeId, uploadBatchId, uploadedBy);
        }

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
    public FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request) {
//         validatePostTotalSize(files, request.fileInfos()., purposeId);
        if (request.fileInfos().size() != files.size()) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_INFO);
        }

        if (request.fileInfos().stream().anyMatch(info -> info.purpose() == FilePurpose.ASSET)) {
            validateSingleAssetZip(files);
            if (request.fileInfos().size() != 1) {
                throw new BusinessException(ErrorCode.NOT_ENOUGH_INFO);
            }

            FileUploadInfo info = request.fileInfos().getFirst();
            User uploadedBy = userRepository.findById(info.uploadedBy())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            return uploadAssetZip(files.getFirst(), info.purposeId(), info.uploadBatchId(), uploadedBy);
        }

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
        File downloadFile = resolveDownloadFile(file);
        return s3FileStorageService.createDownloadPresignedUrl(downloadFile.getS3Key(), downloadFile.getOriginalName());
    }

    @Override
    public void deleteStorageObject(String s3Key) {
        s3FileStorageService.delete(s3Key);
    }

    @Override
    @Transactional
    public void deleteFilesByPurpose(FilePurpose purpose, Long purposeId) {
        List<File> files = fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(
            purpose,
            purposeId
        );

        deleteFileEntities(files);
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
    public List<FileAttachmentResponse> getFileAttachmentsByPurposeAndFileType(
        FilePurpose purpose,
        Long purposeId,
        AssetFileType fileType
    ) {
        return fileRepository.findByPurposeAndPurposeIdAndFileTypeAndDeletedAtIsNullOrderByUploadOrderAsc(
                purpose,
                purposeId,
                fileType
            )
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
        deleteFileEntities(deleteFiles);
    }

    private void deleteFileEntities(List<File> files) {
        if (files.isEmpty()) {
            return;
        }

        files.forEach(file -> file.markDeletedWithRetention(STORAGE_RETENTION));
    }

    private File resolveDownloadFile(File file) {
        if (file.getPurpose() != FilePurpose.ASSET || file.getFileType() == AssetFileType.ZIP) {
            return file;
        }

        return fileRepository.findByPurposeAndPurposeIdAndUploadBatchIdAndFileTypeAndDeletedAtIsNull(
                file.getPurpose(),
                file.getPurposeId(),
                file.getUploadBatchId(),
                AssetFileType.ZIP
            )
            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
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

    private FileUploadResponse uploadAssetZip(MultipartFile file,
                                              Long postId,
                                              UUID uploadBatchId,
                                              User uploadedBy) {
        fileValidator.validateZip(file);
        validateFilesTotalSize(List.of(file), FilePurpose.ASSET, postId);

        Path tempRoot = null;
        List<String> uploadedS3Keys = new ArrayList<>();

        try {
            tempRoot = Files.createTempDirectory("teabag-asset-upload-");
            Path zipPath = tempRoot.resolve(uploadBatchId + ".zip");
            copyMultipartFile(file, zipPath);

            String originalZipKey = s3FileKeyGenerator.generatePostOriginalZip(postId, uploadBatchId);
            s3FileStorageService.upload(zipPath, originalZipKey, contentTypeOrDefault(file.getContentType(), "application/zip"));
            uploadedS3Keys.add(originalZipKey);

            Path extractDir = tempRoot.resolve("extract");
            ZipExtractService.ZipExtractResult extractResult = zipExtractService.extractAssetZip(zipPath, extractDir);

            List<FileResponse> responses = new ArrayList<>();
            responses.add(saveFileMetadata(
                file.getOriginalFilename(),
                originalZipKey,
                FileValidator.ZIP_ALLOWED_EXTENSION,
                file.getSize(),
                FilePurpose.ASSET,
                postId,
                uploadedBy,
                1L,
                contentTypeOrDefault(file.getContentType(), "application/zip"),
                AssetFileType.ZIP,
                uploadBatchId
            ));

            ZipExtractService.ExtractedAssetFile model = extractResult.model();
            String modelKey = s3FileKeyGenerator.generatePostViewerModel(postId);
            s3FileStorageService.upload(model.path(), modelKey, model.contentType());
            uploadedS3Keys.add(modelKey);
            responses.add(saveExtractedAssetMetadata(model, modelKey, FilePurpose.ASSET, postId, uploadedBy, 2L, uploadBatchId));

            long uploadOrder = 3L;
            for (ZipExtractService.ExtractedAssetFile texture : extractResult.textures()) {
                String textureKey = s3FileKeyGenerator.generatePostViewerTexture(postId, texture.originalName());
                s3FileStorageService.upload(texture.path(), textureKey, texture.contentType());
                uploadedS3Keys.add(textureKey);
                responses.add(saveExtractedAssetMetadata(texture, textureKey, FilePurpose.ASSET, postId, uploadedBy, uploadOrder, uploadBatchId));
                uploadOrder++;
            }

            return new FileUploadResponse(responses);
        } catch (BusinessException e) {
            deleteUploadedS3Keys(uploadedS3Keys);
            throw e;
        } catch (RuntimeException e) {
            deleteUploadedS3Keys(uploadedS3Keys);
            throw e;
        } catch (IOException e) {
            deleteUploadedS3Keys(uploadedS3Keys);
            throw new BusinessException(ErrorCode.ZIP_INVALID);
        } finally {
            deleteTempDirectory(tempRoot);
        }
    }

    private FileResponse saveExtractedAssetMetadata(ZipExtractService.ExtractedAssetFile extractedFile,
                                                    String s3Key,
                                                    FilePurpose purpose,
                                                    Long purposeId,
                                                    User uploadedBy,
                                                    Long uploadOrder,
                                                    UUID uploadBatchId) {
        return saveFileMetadata(
            extractedFile.originalName(),
            s3Key,
            extractedFile.extension(),
            extractedFile.sizeBytes(),
            purpose,
            purposeId,
            uploadedBy,
            uploadOrder,
            extractedFile.contentType(),
            extractedFile.fileType(),
            uploadBatchId
        );
    }

    private FileResponse saveFileMetadata(String originalName,
                                          String s3Key,
                                          String extension,
                                          Long sizeBytes,
                                          FilePurpose purpose,
                                          Long purposeId,
                                          User uploadedBy,
                                          Long uploadOrder,
                                          String contentType,
                                          AssetFileType fileType,
                                          UUID uploadBatchId) {
        File storedFile = File.builder()
            .originalName(originalName)
            .s3Key(s3Key)
            .extension(extension)
            .sizeBytes(sizeBytes)
            .purpose(purpose)
            .purposeId(purposeId)
            .fileType(fileType)
            .uploadBatchId(uploadBatchId.toString())
            .uploadOrder(uploadOrder)
            .contentType(contentType)
            .uploadedBy(uploadedBy)
            .build();

        return FileResponse.from(fileRepository.save(storedFile));
    }

    private void validateSingleAssetZip(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_FILE);
        }
    }

    private void copyMultipartFile(MultipartFile file, Path targetPath) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath);
        }
    }

    private String contentTypeOrDefault(String contentType, String defaultContentType) {
        if (contentType == null || contentType.isBlank()) {
            return defaultContentType;
        }

        return contentType;
    }

    private void deleteUploadedS3Keys(List<String> uploadedS3Keys) {
        for (String s3Key : uploadedS3Keys) {
            try {
                s3FileStorageService.delete(s3Key);
            } catch (Exception deleteException) {
                log.warn("Failed to compensate uploaded S3 object. s3Key = {}", s3Key, deleteException);
            }
        }
    }

    private void deleteTempDirectory(Path tempRoot) {
        if (tempRoot == null || !Files.exists(tempRoot)) {
            return;
        }

        try (var paths = Files.walk(tempRoot)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp upload file. path = {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to delete temp upload directory. path = {}", tempRoot, e);
        }
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
