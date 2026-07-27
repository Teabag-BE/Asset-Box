package io.teabag.assetbox.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.Paging;
import io.teabag.assetbox.user.dto.SearchUserByAdminResponse;
import io.teabag.assetbox.user.dto.UserUpdateRoleRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin User API", description = "관리자가 사용하는 사용자 관리 API")
public interface AdminUserSwaggerSupporter {

    @Operation(
            summary = "어드민 유저 목록 조회",
            description = "관리자 권한으로 유저 목록을 검색하고 조회하는 API",
            parameters = {
                    @Parameter(name = "page", description = "검색할 페이지 번호"),
                    @Parameter(name = "size", description = "페이지당 조회할 크기"),
                    @Parameter(name = "role", description = "권한 필터. USER, ADMIN, SUPER_ADMIN"),
                    @Parameter(name = "q", description = "닉네임 또는 이메일 검색어")
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "어드민 유저 목록 조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "성공적으로 계정이 조회되었습니다.",
                        "data": {
                            "items": [
                                {
                                    "id": 1,
                                    "email": "user@example.com",
                                    "nickname": "asset-user",
                                    "major": "BACK_END",
                                    "provider": "LOCAL",
                                    "role": "USER",
                                    "isOauthLinked": false,
                                    "postCount": 7,
                                    "totalLikes": 23
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1,
                            "first": true,
                            "last": true
                        },
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 어드민 권한 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<SearchUserByAdminResponse>> getUserDetails(
            @Parameter(hidden = true) CurrentUser currentUser,
            Paging paging,
            String role,
            String q
    );

    @Operation(summary = "유저 권한 변경", description = "SUPER_ADMIN 권한으로 특정 유저의 권한을 변경하는 API")
    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserUpdateRoleRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 권한 변경 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "성공적으로 계정의 역할이 변경되었습니다.",
                        "data": null,
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "동일 권한 변경 또는 본인 권한 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "루트 어드민 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<Void>> switchToAdmin(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Parameter(description = "권한을 변경할 사용자 ID", required = true) Long id,
            UserUpdateRoleRequest request
    );
}
