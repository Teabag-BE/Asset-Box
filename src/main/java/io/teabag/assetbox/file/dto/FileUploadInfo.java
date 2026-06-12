package io.teabag.assetbox.file.dto;

import java.util.UUID;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.user.domain.User;

public record FileUploadInfo(FilePurpose purpose,
                             Long purposeId,
                             AssetFileType fileType,
                             UUID uploadBatchId,
                             Long sortOrder,
                             Long uploadedBy) {
}
