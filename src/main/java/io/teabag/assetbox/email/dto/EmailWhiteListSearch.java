package io.teabag.assetbox.email.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.util.List;

public record EmailWhiteListSearch(
        String major,
        String name,
        String email,
        String status
) {
    @QueryProjection
    public EmailWhiteListSearch{}
}
