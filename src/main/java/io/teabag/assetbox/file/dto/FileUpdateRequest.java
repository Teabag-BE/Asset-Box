package io.teabag.assetbox.file.dto;

import java.util.List;

public record FileUpdateRequest(
        List<Long> cFileSortOrders,
        List<FileURequest> uRequest,
        List<Long> dFileIds
) {
}
