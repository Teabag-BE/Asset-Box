package io.teabag.assetbox.email.controller;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.email.dto.EmailVerificationRequest;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.email.dto.EnrollEmailResponse;
import io.teabag.assetbox.email.service.EmailService;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
public class EmailController {
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<ApiResponse<EnrollEmailResponse>> enrollEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody @Valid EnrollEmailRequest request
            ){
        return ResponseEntity.ok(
            ApiResponse.created(
                    emailService.enrollEmail(
                            currentUser.getEmail(),
                            request
                    ),
                    SuccessCode.MAIL_ENROLL_COMPLETE.getSuccessMessage()
            )
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> startVerification(
            @RequestBody EmailVerificationRequest request
    ){
        emailService.checkAvailableEmail(request.email());
        return ResponseEntity.ok(
                ApiResponse.ok(
                        SuccessCode.MAIL_SENEDED.getSuccessMessage()
                )
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @RequestParam String token
    ){
        emailService.verifyToken(token);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        SuccessCode.MAIL_SENEDED.getSuccessMessage()
                )
        );
    }

}
