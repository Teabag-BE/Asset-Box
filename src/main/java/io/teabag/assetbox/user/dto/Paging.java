package io.teabag.assetbox.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;

public record Paging(
       @NotNull int page,
       @NotNull int size
) {
    public PageRequest toPageable(){
        return PageRequest.of(page, size);
    }
}
