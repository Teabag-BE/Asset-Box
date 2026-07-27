package io.teabag.assetbox.email.dto;

import io.teabag.assetbox.email.domain.EmailWhiteList;

public record EnrollEmailResponse(
        String email,
        String name,
        String major,
        String status
) {
    public static EnrollEmailResponse from(EmailWhiteList emailWhiteList){
        return new EnrollEmailResponse(
                emailWhiteList.getEmail(),
                emailWhiteList.getName(),
                emailWhiteList.getMajor().toString(),
                emailWhiteList.getEmailStatus().toString()
        );
    }
}
