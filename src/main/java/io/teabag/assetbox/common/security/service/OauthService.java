package io.teabag.assetbox.common.security.service;

import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.security.domain.RefreshToken;
import io.teabag.assetbox.common.security.repository.RefreshTokenRepository;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.RefreshResponse;
import io.teabag.assetbox.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Map;

@Slf4j
@Service
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class OauthService extends DefaultOAuth2UserService {

    private final String REFRESH_TOKEN_NAME = "RT";
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerId = userRequest.getClientRegistration().getRegistrationId();

        String extactedEmail = getEmailFromOauth2user(providerId, oAuth2User);

        User foundedUser;

        // 회원가입 되지 않은 계정인 경우
        try{
            foundedUser = userRepository.findByEmailOrThrow(extactedEmail);
        } catch (BusinessException e){
            throw new OAuth2AuthenticationException(ErrorCode.NOT_REGISTERED.toString());
        }

        if(providerId.equalsIgnoreCase("GOOGLE")) providerId = "gmail";

        // 계정에 등록된 Provider와 다른 Provider인 경우
        if( !foundedUser.getProvider().equals(providerId) ){
            throw new OAuth2AuthenticationException(ErrorCode.NOT_SAME_ORIGIN_PROVIDER.toString());
        }

        return CurrentUser.from(foundedUser);
     }

    public String getEmailFromOauth2user(
            String provider,
            OAuth2User oAuth2User
    ){

        return switch( provider.toUpperCase() ){
            case "GOOGLE" -> oAuth2User.getAttribute("email").toString();
            case "NAVER" -> {
                Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttribute("response");
                yield response.get("email").toString();
            }
            default -> throw new BusinessException(ErrorCode.NOT_SUPPORTED_OAUTH_PROVIDER);
        };
    }


    public KeyPair refreshToken(String refreshToken){
        // NPE 검증
        PreConditions.validate(
                Strings.isNotBlank(refreshToken),
                ErrorCode.TOKEN_NOT_FOUND
        );

        // 토큰 유효성 검증
        tokenProvider.validate(refreshToken);

        // 토큰 화이트리스트 검증
        RefreshToken foundedRT = refreshTokenRepository.findById(refreshToken)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED)
                );

        // 토큰 재발급
        refreshTokenRepository.delete(foundedRT);
        User foundedUser = userRepository.findByEmailOrThrow(foundedRT.getEmail());
        return tokenProvider.issueKeyPair(
                foundedUser.getEmail(),
                foundedUser.getRole()
        );
    }
}
