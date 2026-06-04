package io.teabag.assetbox.file.service.upload;

import java.util.UUID;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.dto.FileResponse;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileRepository fileRepository;
    private final S3FileUploadStorageService s3FileUploadStorageService;
    private final FileUploadValidator fileUploadValidator;
    private final S3FileKeyGenerator s3FileKeyGenerator;

    @Override
    public FileResponse upload(MultipartFile file, FilePurpose purpose, Long purposeId, AssetFileType fileType,
        UUID uploadBatchId, Long uploadOrder, User uploadedBy) {
        fileUploadValidator.validate(file);

        String originalName = file.getOriginalFilename();
        String extension = fileUploadValidator.extractExtension(originalName);

        String s3Key = s3FileKeyGenerator.generate(
            purpose,
            purposeId,
            fileType,
            uploadBatchId,
            originalName
        );

        s3FileUploadStorageService.upload(file, s3Key);

        File storedFile = File.builder()
            .originalName(originalName)
            .savedName(s3Key)
            .extension(extension)
            .sizeBytes(file.getSize())
            .purpose(purpose)
            .purposeId(purposeId)
            .fileType(fileType)
            .uploadBatchId(uploadBatchId.toString())
            .uploadOrder(uploadOrder)
            .uploadedBy(uploadedBy)
            .build();

        // DB에 파일 메타데이터 저장
        File savedFile = fileRepository.save(storedFile);

        // 파일 응답 형식 반환
        return FileResponse.from(savedFile);
    }
}
