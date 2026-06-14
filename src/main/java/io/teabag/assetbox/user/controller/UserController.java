package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.properties.JwtProperties;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.security.service.OauthService;
import io.teabag.assetbox.user.constants.Provider;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final OauthService oauthService;
    private final JwtProperties jwtProperties;
    private static final String REFRESH_TOKEN_NAME = "RT";
    private static final String BEARER_KEYWORD = "Bearer";


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserCreateResponse>> signUp(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                        .body(
                                ApiResponse.created(
                                        userService.signup(request),
                                        SuccessCode.USER_CREATED.getSuccessMessage()
                                )
                        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> signIn(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {

        KeyPair keyPair = userService.signIn(request);

        Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, keyPair.refreshToken());
        cookie.setMaxAge(jwtProperties.getValidations().getRefresh() / 1000);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiResponse.created(
                                LoginResponse.builder()
                                        .tokenType(BEARER_KEYWORD)
                                        .accessToken(keyPair.accessToken())
                                        .build(),
                                SuccessCode.USER_SIGNIN.getSuccessMessage()
                        )
                );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getUser(
            @AuthenticationPrincipal CurrentUser currentUser
    ){
        String email = currentUser.getEmail();

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                userService.getMyInfo(email),
                                SuccessCode.USER_READ.getSuccessMessage()
                        )
                );
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<MyInfoResponse>>saveAvatar(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestPart("file") MultipartFile file
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                userService.saveAvatar(currentUser.getEmail(),file),
                                SuccessCode.USER_READ.getSuccessMessage()
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                userService.getUserProfile(id, currentUser.getRole()),
                                SuccessCode.USER_READ.getSuccessMessage()
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String requestToken = resolveRefreshToken(request);

        KeyPair keyPair = oauthService.refreshToken(requestToken);

        Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, keyPair.refreshToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(jwtProperties.getValidations().getRefresh() / 1000);
        response.addCookie(cookie);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        RefreshResponse.builder().accessToken(keyPair.accessToken()).tokenType("Bearer").build(),
                        SuccessCode.TOKEN_REFRESH_COMPLETED.getSuccessMessage()
                )
        );
    }


    @GetMapping("/oauth2/authorization/{provider}")
    public void redirectToOauth(
            @PathVariable(required = true) String provider,
            HttpServletResponse httpServletResponse
    ) throws IOException {
        Provider providedProvider;

        try {
            providedProvider = Provider.valueOf(provider.toUpperCase());
        } catch(Exception e){
            throw new BusinessException(ErrorCode.NOT_VALID_PROVIDER);
        }

        String toStrProvider = providedProvider.name().toLowerCase();


        httpServletResponse.sendRedirect("/oauth2/authorization/" + toStrProvider);
    }


    public String resolveRefreshToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies.length > 0){
            for(Cookie cookie : cookies){
                if(REFRESH_TOKEN_NAME.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>>updateMyInfo(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                userService.updateMyInfo(currentUser.getEmail(),request),
                                SuccessCode.USER_UPDATED.getSuccessMessage()
                        )
                );
    }


}
