package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.file.domain.AssetFile;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.dto.FileResponse;
import io.teabag.assetbox.file.repository.AssetFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FileService {

    private final AssetFileRepository assetFileRepository;
    private final FileStorageService fileStorageService;

    public FileService(AssetFileRepository assetFileRepository, FileStorageService fileStorageService) {
        this.assetFileRepository = assetFileRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public FileResponse save(FilePurpose purpose, Long ownerId, Long uploadedBy, MultipartFile file) {
        String storedPath = fileStorageService.save(file);
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "";
        AssetFile saved = assetFileRepository.save(new AssetFile(originalName, storedPath, extension, file.getSize(), purpose, ownerId, uploadedBy));
        return FileResponse.from(saved);
    }

    public FileResponse meta(Long fileId) {
        AssetFile file = assetFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND, "File not found"));
        return FileResponse.from(file);
    }
}
