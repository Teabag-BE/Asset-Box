package io.teabag.assetbox.email.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.email.dto.DeleteEmailRequest;
import io.teabag.assetbox.email.dto.EmailWhiteListSearch;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.email.dto.EnrollEmailResponse;
import io.teabag.assetbox.email.service.EmailService;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.Paging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class AdminEmailController implements AdminEmailSwaggerSupporter{
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<ApiResponse<EnrollEmailResponse>> enrollEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody @Valid EnrollEmailRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        emailService.enrollEmail(
                                currentUser.getEmail(),
                                request
                        ),
                        SuccessCode.MAIL_ENROLL_COMPLETE.getSuccessMessage()
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmailWhiteListSearch>>> getSearches(
            @AuthenticationPrincipal CurrentUser currentUser,
            Paging paging
    ){
        return ResponseEntity.ok(
                ApiResponse.ok(
                        emailService.getSearches(currentUser.getEmail(),paging.toPageable()),
                        SuccessCode.MAIL_WHITELIST_SEARCH_COMPLETE.getSuccessMessage()
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody DeleteEmailRequest request
    ){
        emailService.deleteEmailFromWhiteList(
                currentUser.getEmail(),
                request.email()
        );
        return ResponseEntity.ok(
                ApiResponse.ok(
                        SuccessCode.MAIL_DELETE_COMPLETE.getSuccessMessage()
                )
        );
    }
}
