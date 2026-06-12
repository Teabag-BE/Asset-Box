package io.teabag.assetbox.file.dto;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.user.domain.User;

public record FileUploadRequest(
	List<FileUploadInfo> fileInfos
) {
}
