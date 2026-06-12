package io.teabag.assetbox.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;

public record Paging(
       @NotBlank int page,
       @NotBlank int size
) {
    public PageRequest toPageable(){
        return PageRequest.of(page-1, size);
    }
}
