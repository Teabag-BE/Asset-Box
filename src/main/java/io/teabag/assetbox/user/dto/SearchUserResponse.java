package io.teabag.assetbox.user.dto;


import lombok.Builder;

import java.util.List;

@Builder
public record SearchUserResponse(
        List<UserInfoResponse> items,
        Integer page,
        Integer size,
        Integer totalElements,
        Integer totalPages,
        boolean first,
        boolean last
) {
}
