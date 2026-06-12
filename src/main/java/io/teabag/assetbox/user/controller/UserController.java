package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.constants.TokenType;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.JwtProperties;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.user.dto.LoginRequest;
import io.teabag.assetbox.user.dto.LoginResponse;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
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
}
