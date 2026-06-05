package io.teabag.assetbox.file.service.upload;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.File;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileRepository fileRepository;
    private final S3FileUploadStorageService s3FileUploadStorageService;
    private final FileUploadValidator fileUploadValidator;
    private final S3FileKeyGenerator s3FileKeyGenerator;
    private final UserRepository userRepository;


    @Override
    public FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request) {
        List<FileResponse> uploadInfos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            List<FileUploadInfo> infos = request.fileInfos();
            FileResponse uploadInfo = upload(files.get(i), infos.get(i));
            uploadInfos.add(uploadInfo);
        }
        return new FileUploadResponse(uploadInfos);
    }


    // @Override
    @Transactional
    public FileResponse upload(MultipartFile file, FileUploadInfo info) {
        //FilePurpose purpose, Long purposeId, AssetFileType fileType,
        //         UUID uploadBatchId, Long uploadOrder, User uploadedBy
        fileUploadValidator.validate(file);

        String originalName = file.getOriginalFilename();
        String extension = fileUploadValidator.extractExtension(originalName);

        String s3Key = s3FileKeyGenerator.generate(
            info.purpose(),
            info.purposeId(),
            info.fileType(),
            info.uploadBatchId(),
            originalName
        );

        s3FileUploadStorageService.upload(file, s3Key);
        log.info("Uploaded file {} with extension {} :: {}", originalName, extension,  s3Key);


        User uploadedBy = userRepository.findById(info.uploadedBy()).orElseThrow(() -> new BusinessException(
                ErrorCode.POST_NOT_FOUND));
        File storedFile = File.builder()
            .originalName(originalName)
            .savedName(s3Key)
            .extension(extension)
            .sizeBytes(file.getSize())
            .purpose(info.purpose())
            .purposeId(info.purposeId())
            .fileType(info.fileType())
            .uploadBatchId(info.uploadBatchId().toString())
            .uploadOrder(info.sortOrder())
            .uploadedBy(uploadedBy)
            .build();

        // DB에 파일 메타데이터 저장
        File savedFile = fileRepository.save(storedFile);

        // 파일 응답 형식 반환
        return FileResponse.from(savedFile);
    }


}
