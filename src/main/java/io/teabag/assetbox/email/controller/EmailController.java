package io.teabag.assetbox.email.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.email.dto.*;
import io.teabag.assetbox.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
public class EmailController {
    private final EmailService emailService;

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
                        SuccessCode.MAIL_VERIFICATION_COMPLETE.getSuccessMessage()
                )
        );
    }
}
