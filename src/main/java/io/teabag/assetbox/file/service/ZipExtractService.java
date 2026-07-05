package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipExtractService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("fbx", "png", "jpg", "jpeg");
    private static final long DEFAULT_MAX_EXTRACTED_TOTAL_SIZE_BYTES = 100 * 1024 * 1024L;
    private static final long DEFAULT_MAX_EXTRACTED_FILE_SIZE_BYTES = 50 * 1024 * 1024L;
    private static final int DEFAULT_MAX_EXTRACTED_FILE_COUNT = 100;

    private final long maxExtractedTotalSizeBytes;
    private final long maxExtractedFileSizeBytes;
    private final int maxExtractedFileCount;

    public ZipExtractService() {
        this(
            DEFAULT_MAX_EXTRACTED_TOTAL_SIZE_BYTES,
            DEFAULT_MAX_EXTRACTED_FILE_SIZE_BYTES,
            DEFAULT_MAX_EXTRACTED_FILE_COUNT
        );
    }

    ZipExtractService(
        long maxExtractedTotalSizeBytes,
        long maxExtractedFileSizeBytes,
        int maxExtractedFileCount
    ) {
        this.maxExtractedTotalSizeBytes = maxExtractedTotalSizeBytes;
        this.maxExtractedFileSizeBytes = maxExtractedFileSizeBytes;
        this.maxExtractedFileCount = maxExtractedFileCount;
    }

    public ZipExtractResult extractAssetZip(Path zipFile, Path extractDir) {
        Path normalizedExtractDir = extractDir.toAbsolutePath().normalize();
        List<ExtractedAssetFile> modelFiles = new ArrayList<>();
        List<ExtractedAssetFile> textureFiles = new ArrayList<>();
        Set<Path> extractedPaths = new HashSet<>();
        int extractedFileCount = 0;
        long totalExtractedSize = 0L;

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

                    if (!extractedPaths.add(targetPath)) {
                        throw new BusinessException(ErrorCode.ZIP_INVALID);
                    }

                    extractedFileCount++;
                    if (extractedFileCount > maxExtractedFileCount) {
                        throw new BusinessException(ErrorCode.ZIP_INVALID);
                    }

                    String originalName = extractFilename(entry.getName());
                    String extension = extractExtension(originalName);
                    validateAllowedExtension(extension);

                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    long extractedSize = copyEntry(zipInputStream, targetPath, totalExtractedSize);
                    totalExtractedSize += extractedSize;

                    ExtractedAssetFile extractedFile = new ExtractedAssetFile(
                        targetPath,
                        originalName,
                        extension,
                        extractedSize,
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

    private long copyEntry(ZipInputStream zipInputStream, Path targetPath, long currentTotalSize) throws IOException {
        long extractedSize = 0L;
        byte[] buffer = new byte[8192];

        try (OutputStream outputStream = Files.newOutputStream(
            targetPath,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )) {
            int read;
            while ((read = zipInputStream.read(buffer)) != -1) {
                extractedSize += read;
                if (extractedSize > maxExtractedFileSizeBytes) {
                    throw new BusinessException(ErrorCode.SIZE_INVALID);
                }

                if (currentTotalSize + extractedSize > maxExtractedTotalSizeBytes) {
                    throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_INVALID);
                }

                outputStream.write(buffer, 0, read);
            }
        }

        return extractedSize;
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
