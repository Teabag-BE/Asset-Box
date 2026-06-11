package io.teabag.assetbox.file.service;

import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 파일 업로드
public interface FileService {

	/*upload*/
	FileUploadResponse uploadFiles(List<MultipartFile> files, FileUploadRequest request);
	String uploadThumbnail(MultipartFile file, ThumbnailPurpose purpose, Long purposeId);

	/*download*/
	String getDownloadPresignedUrl(String fileName);
}
