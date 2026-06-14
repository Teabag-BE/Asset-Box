package io.teabag.assetbox.user.dto.directory;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestParam;

public record SearchUserRequest(
      String sortColumn,
      String sortType
) {
}
