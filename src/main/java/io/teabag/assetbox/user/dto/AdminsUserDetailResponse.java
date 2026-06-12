package io.teabag.assetbox.user.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminsUserDetailResponse(
    List<UserDetailsResponse> items,
    Integer page,
    Integer size,
    Integer totalElements,
    Integer totalPages,
    boolean first,
    boolean last
) {

}
