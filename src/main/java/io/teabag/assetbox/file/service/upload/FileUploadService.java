package io.teabag.assetbox.file.service.upload;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.dto.FileResponse;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.user.domain.User;

// 파일 업로드
public interface FileUploadService {

	// FileResponse upload(
	// 	MultipartFile file,
	// 	FilePurpose purpose,
	// 	Long purposeId,
	// 	AssetFileType fileType,
	// 	UUID uploadBatchId,
	// 	Long sortOrder,
	// 	User uploadedBy
	// );

	FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request);
}
