package io.teabag.assetbox.common.eventhandler;

import io.teabag.assetbox.common.properties.JwtProperties;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.common.security.service.TokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OauthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;
    private static final String REFRESH_TOKEN_NAME = "RT";
    //
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        CurrentUser principal;
        if(authentication.getPrincipal() instanceof CurrentUser c) principal = c;
        else {
            throw new BusinessException(
                    ErrorCode.AUTHENTICATION_ERROR,
                    "Authentication 객체에서 형변환 중 오류가 발생했습니다."
            );
        }
        //
        KeyPair keyPair = tokenProvider.issueKeyPair(
                principal.getEmail(),
                principal.getRole()
        );
        //
        Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, keyPair.refreshToken());
        cookie.setMaxAge(jwtProperties.getValidations().getRefresh() / 1000);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        //
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        //
        objectMapper.writeValue(
                response.getWriter(),
                Map.of(
                    "success", true,
                    "data", Map.of(
                            "accessToken", keyPair.accessToken(),
                                "tokenType", "Bearer"
                        )
                )
        );
    }
}
