package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipExtractService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("fbx", "png", "jpg", "jpeg");

    public ZipExtractResult extractAssetZip(Path zipFile, Path extractDir) {
        Path normalizedExtractDir = extractDir.toAbsolutePath().normalize();
        List<ExtractedAssetFile> modelFiles = new ArrayList<>();
        List<ExtractedAssetFile> textureFiles = new ArrayList<>();

        try {
            Files.createDirectories(normalizedExtractDir);

            try (InputStream inputStream = Files.newInputStream(zipFile);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;

                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory() || isIgnoredSystemEntry(entry.getName())) {
                        zipInputStream.closeEntry();
                        continue;
                    }

                    Path targetPath = normalizedExtractDir.resolve(entry.getName()).normalize();
                    if (!targetPath.startsWith(normalizedExtractDir)) {
                        throw new BusinessException(ErrorCode.ZIP_INVALID);
                    }

                    String originalName = extractFilename(entry.getName());
                    String extension = extractExtension(originalName);
                    validateAllowedExtension(extension);

                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    ExtractedAssetFile extractedFile = new ExtractedAssetFile(
                        targetPath,
                        originalName,
                        extension,
                        Files.size(targetPath),
                        contentType(extension),
                        fileType(extension)
                    );

                    if (extractedFile.fileType() == AssetFileType.MODEL) {
                        modelFiles.add(extractedFile);
                    } else {
                        textureFiles.add(extractedFile);
                    }

                    zipInputStream.closeEntry();
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.ZIP_INVALID);
        }

        if (modelFiles.isEmpty()) {
            throw new BusinessException(ErrorCode.ZIP_MODEL_NOT_FOUND);
        }

        if (modelFiles.size() > 1) {
            throw new BusinessException(ErrorCode.ZIP_MODEL_COUNT_INVALID);
        }

        return new ZipExtractResult(modelFiles.getFirst(), textureFiles);
    }

    private boolean isIgnoredSystemEntry(String entryName) {
        String normalizedName = entryName.replace('\\', '/');
        String fileName = extractFilename(normalizedName);

        return normalizedName.startsWith("__MACOSX/")
            || fileName.equals(".DS_Store")
            || fileName.isBlank();
    }

    private void validateAllowedExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.EXTENSIONS_INVALID);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new BusinessException(ErrorCode.EXTENSIONS_INVALID);
        }

        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private String extractFilename(String entryName) {
        String normalizedName = entryName.replace('\\', '/');
        return normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
    }

    private String contentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private AssetFileType fileType(String extension) {
        if (FileValidator.MODEL_ALLOWED_EXTENSION.equals(extension)) {
            return AssetFileType.MODEL;
        }

        return AssetFileType.TEXTURE;
    }

    public record ZipExtractResult(
        ExtractedAssetFile model,
        List<ExtractedAssetFile> textures
    ) {
    }

    public record ExtractedAssetFile(
        Path path,
        String originalName,
        String extension,
        long sizeBytes,
        String contentType,
        AssetFileType fileType
    ) {
    }
}
