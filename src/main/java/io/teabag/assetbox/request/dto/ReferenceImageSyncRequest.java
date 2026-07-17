package io.teabag.assetbox.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ReferenceImageSyncRequest(
        @NotNull List<@Valid ExistingImage> existingImages,
        @NotNull List<@NotNull @Positive Long> newFileSortOrders
) {
    public record ExistingImage(
            @NotNull @Positive Long fileId,
            @NotNull @Positive Long sortOrder
    ) {
    }
}
