package io.teabag.assetbox.file.dto;

import java.util.List;

public record FileUploadRequest(
	List<FileUploadInfo> fileInfos
) {
}
