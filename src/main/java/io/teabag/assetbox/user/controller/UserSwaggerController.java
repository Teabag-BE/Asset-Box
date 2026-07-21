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
import io.teabag.assetbox.user.dto.LoginRequest;
import io.teabag.assetbox.user.dto.LoginResponse;
import io.teabag.assetbox.user.dto.MyInfoResponse;
import io.teabag.assetbox.user.dto.Paging;
import io.teabag.assetbox.user.dto.RefreshResponse;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.dto.UserProfileResponse;
import io.teabag.assetbox.user.dto.UserUpdateRequest;
import io.teabag.assetbox.user.dto.directory.SearchUserRequest;
import io.teabag.assetbox.user.dto.directory.SearchUserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "User API", description = "사용자 인증, 프로필, 유저 디렉토리 관련 API")
public interface UserSwaggerController {

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름, 닉네임, 전공으로 회원가입하는 API")
    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SignupRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "계정이 정상적으로 생성되었습니다.",
                        "data": {
                            "id": 1,
                            "email": "user@example.com",
                            "name": "홍길동",
                            "nickname": "asset-user",
                            "major": "BACK_END",
                            "provider": "LOCAL",
                            "role": "USER"
                        },
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "가입 허용 이메일이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 이메일")
    })
    ResponseEntity<ApiResponse<UserCreateResponse>> signUp(SignupRequest request);

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 Access Token을 발급하는 API")
    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "계정에 성공적으로 로그인되었습니다",
                        "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                            "tokenType": "Bearer"
                        },
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 실패")
    })
    ResponseEntity<ApiResponse<LoginResponse>> signIn(LoginRequest request, @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "계정이 성공적으로 조회되었습니다",
                        "data": {
                            "id": 1,
                            "email": "user@example.com",
                            "publicEmail": "contact@example.com",
                            "name": "홍길동",
                            "nickname": "asset-user",
                            "major": "BACK_END",
                            "description": "소개글",
                            "provider": "LOCAL",
                            "role": "USER",
                            "avatarUrl": "/api/files/1"
                        },
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<MyInfoResponse>> getUser(@Parameter(hidden = true) CurrentUser currentUser);

    @Operation(summary = "아바타 업로드", description = "현재 로그인한 사용자의 아바타 이미지를 업로드하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아바타 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<MyInfoResponse>> saveAvatar(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Parameter(description = "업로드할 아바타 이미지 파일", required = true) MultipartFile file
    );

    @Operation(summary = "특정 유저 프로필 조회", description = "사용자 ID로 유저 프로필을 조회하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @Parameter(description = "조회할 사용자 ID", required = true) Long id,
            @Parameter(hidden = true) CurrentUser currentUser
    );

    @Operation(summary = "토큰 재발급", description = "Refresh Token 쿠키를 검증하고 Access Token을 재발급하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "토큰이 성공적으로 재발급되었습니다.",
                        "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                            "tokenType": "Bearer"
                        },
                        "error": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 누락 또는 만료")
    })
    ResponseEntity<ApiResponse<RefreshResponse>> refreshToken(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(summary = "OAuth 로그인 시작", description = "지원하는 OAuth 제공자의 인증 페이지로 리다이렉트하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "OAuth 제공자 인증 페이지로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 OAuth 제공자")
    })
    void redirectToOauth(
            @Parameter(description = "OAuth 제공자. google 또는 naver", required = true) String provider,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse
    ) throws IOException;

    @Operation(
            summary = "유저 디렉토리 조회",
            description = "닉네임/이름/전공 조건과 정렬 조건으로 유저 디렉토리를 조회하는 API",
            parameters = {
                    @Parameter(name = "q", description = "닉네임 또는 이름 검색어"),
                    @Parameter(name = "major", description = "전공 필터"),
                    @Parameter(name = "page", description = "검색할 페이지 번호"),
                    @Parameter(name = "size", description = "페이지당 조회할 크기"),
                    @Parameter(name = "sortColumn", description = "정렬 컬럼"),
                    @Parameter(name = "sortType", description = "정렬 방향")
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 디렉토리 조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "계정이 성공적으로 조회되었습니다",
                        "data": {
                            "items": [
                                {
                                    "id": 1,
                                    "name": "홍길동",
                                    "nickname": "asset-user",
                                    "imageUrl": "/api/files/1",
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지네이션 또는 정렬 입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<SearchUserResponse>> get(
            String q,
            String major,
            Paging paging,
            SearchUserRequest request
    );

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 닉네임, 전공, 공개 이메일, 소개글을 수정하는 API")
    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserUpdateRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<ApiResponse<MyInfoResponse>> updateMyInfo(
            UserUpdateRequest request,
            @Parameter(hidden = true) CurrentUser currentUser
    );
}
