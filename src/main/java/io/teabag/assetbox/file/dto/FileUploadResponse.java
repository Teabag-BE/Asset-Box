package io.teabag.assetbox.file.dto;

import java.util.List;

public record FileUploadResponse(
	List<FileResponse> files
) {
}
