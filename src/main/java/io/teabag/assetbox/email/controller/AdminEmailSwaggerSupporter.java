package io.teabag.assetbox.email.controller;

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
import io.teabag.assetbox.email.dto.DeleteEmailRequest;
import io.teabag.assetbox.email.dto.EmailWhiteListSearch;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.email.dto.EnrollEmailResponse;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name="Admin Email API", description="관리자가 사용하는 이메일 관련 API")
public interface AdminEmailSwaggerSupporter {


    @Operation(
            summary = "화이트리스트 이메일 등록",
            description = "화이트리스트 상에 이메일을 등록하는 API"
    )
    @RequestBody(
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EnrollEmailRequest.class
                            )
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장바구니 조회 성공",
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = @ExampleObject(
                                            value = """
                    {
                        "success":true,
                        "message":"이메일이 화이트리스트에 등록되었습니다.",
                        "data":{
                            "email":"whitelist@naver.com",
                            "name":"화이트리스트이용자",
                            "major":"BACK_END",
                            "status":"ENROLL"
                        },
                        "error":null
                    }
                                            """
                                    )
                            )
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "어드민만 검증 가능"
            ),
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<ApiResponse<EnrollEmailResponse>> enrollEmail(
            CurrentUser currentUser,
            EnrollEmailRequest request
    );


    @Operation(
            summary = "화이트리스트 이메일 검색",
            description = "화이트리스트 상에 등록된 이메일을 검색하는 API",
            parameters = {
                    @Parameter(name = "page" , description = "검색할 페이지 번호"),
                    @Parameter(name = "size" , description = "페이지 상 도출할 크기")
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "화이트리스트 상 이메일 조회 성공",
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = @ExampleObject(
                                            value = """
            {
                "success":true,
                "message":"화이트리스트 상 이메일이 정상적으로 조회되었습니다.",
                "data":{
                    "content":[
                        {
                            "major":"BACK_END",
                            "name":"이정수",
                            "email":"wjdtn747@gmail.com",
                            "status":"ENROLL"
                        },
                        {
                            "major":"BACK_END",
                            "name":"화이트리스트이용자0",
                            "email":"whitelist@naver.com0",
                            "status":"ENROLL"
                        },
                        {
                            "major":"BACK_END",
                            "name":"화이트리스트이용자1",
                            "email":"whitelist@naver.com1",
                            "status":"ENROLL"
                        },
                        {
                            "major":"BACK_END",
                            "name":"화이트리스트이용자2",
                            "email":"whitelist@naver.com2",
                            "status":"ENROLL"
                        },
                        {
                            "major":"BACK_END",
                            "name":"화이트리스트이용자3",
                            "email":"whitelist@naver.com3",
                            "status":"ENROLL"
                        }
                    ],
                    "empty":false,
                    "first":true,
                    "last":false,
                    "number":0,
                    "numberOfElements":5,
                    "pageable":{
                        "offset":0,
                        "pageNumber":0,
                        "pageSize":5,
                        "paged":true,
                        "sort":{
                            "empty":true,
                            "sorted":false,
                            "unsorted":true
                        },
                        "unpaged":false
                    },
                    "size":5,
                    "sort":{
                        "empty":true,
                        "sorted":false,
                        "unsorted":true
                    },
                    "totalElements":21,
                    "totalPages":5
                },
                "error":null
            }
                                            """
                                    )
                            )
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "어드민만 검증 가능"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<ApiResponse<Page<EmailWhiteListSearch>>> getSearches(
            CurrentUser currentUser,
            Paging paging
    );



    @Operation(
            summary = "화이트리스트 상 이메일 삭제",
            description = "화이트리스트 상에 등록된 이메일을 삭제하는 API"
    )
    @RequestBody(
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeleteEmailRequest.class
                            )
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 삭제 성공",
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = @ExampleObject(
                                            value = """
                    {
                        "success":true,
                        "message":"화이트리스트 상 이메일이 정상적으로 삭제되었습니다.",
                        "data":null,
                        "error":null
                    }
                                            """
                                    )
                            )
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "어드민만 검증 가능"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<ApiResponse<Void>> deleteMember(
            CurrentUser currentUser,
            DeleteEmailRequest request
    );

}
