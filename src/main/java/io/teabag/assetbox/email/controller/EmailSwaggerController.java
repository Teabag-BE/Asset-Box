package io.teabag.assetbox.email.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.email.dto.EmailVerificationRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name="Email API", description="모든 사용자가 사용하는 이메일 관련 API")
public interface EmailSwaggerController {

    @Operation(
            summary = "이메일 인증 메일 발송",
            description = "회원가입 전 화이트리스트에 등록된 이메일로 인증 메일을 발송하는 API"
    )
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = EmailVerificationRequest.class)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 메일 발송 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "이메일에 인증코드가 정상적으로 발송되었습니다.",
                        "data": null,
                        "error": null
                    }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이메일 인증 요청 중복 또는 잘못된 입력"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "이메일 인증 처리 실패"
            )
    })
    ResponseEntity<ApiResponse<Void>> startVerification(
            EmailVerificationRequest request
    );

    @Operation(
            summary = "이메일 인증 완료",
            description = "이메일로 전달된 인증 토큰을 검증하고 화이트리스트 이메일을 인증 완료 처리하는 API",
            parameters = @Parameter(name = "token", description = "이메일 인증 토큰", required = true)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "이메일이 정상적으로 인증되었습니다.",
                        "data": null,
                        "error": null
                    }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "만료되었거나 유효하지 않은 이메일 인증 토큰"
            )
    })
    ResponseEntity<ApiResponse<Void>> verify(
            String token
    );
}
