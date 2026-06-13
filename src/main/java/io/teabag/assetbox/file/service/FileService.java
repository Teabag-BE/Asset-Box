package io.teabag.assetbox.file.service;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.user.domain.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// 파일 업로드
public interface FileService {

	/*upload*/
	String uploadThumbnail(MultipartFile file, ThumbnailPurpose purpose, Long purposeId);
	FileUploadResponse uploadFiles(List<MultipartFile> files,
	                               FilePurpose purpose,
	                               Long purposeId,
	                               AssetFileType fileType,
	                               UUID uploadBatchId,
	                               User uploadedBy);
	FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request);

	/*download*/
	// 파일 미리보기 presignedUrl 조회
	String getShowPresignedUrl(String s3Key);

	// 파일 미리보기 presignedUrl 조회
	List<String> getShowPresignedUrlsByPurpose(String purpose, Long purposeId);

	//파일 이미지 다운로드 presignedUrl 생성
	String getDownloadPresignedUrl(Long fileId);

	// 미리보기 URL + file id까지 같이 내려주는 메서드 (file id는 다운로드 할 때 사용)
	List<FileAttachmentResponse> getFileAttachmentsByPurpose(FilePurpose purpose, Long purposeId);
}
