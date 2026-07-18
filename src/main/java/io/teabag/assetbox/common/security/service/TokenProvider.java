package io.teabag.assetbox.common.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.teabag.assetbox.common.properties.JwtProperties;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.security.domain.RefreshToken;
import io.teabag.assetbox.common.security.repository.RefreshTokenRepository;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.common.constants.TokenType;
import io.teabag.assetbox.user.dto.TokenBody;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenProvider {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Value("${mail.auth-code-expiration-millis}")
    private int jwtEmailExpirationMs;

    private final String EMAIL_VALIDATION_TOKEN_NAME = "validation-token";

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecrets().getAppKey().getBytes());
    }

    private String issueRefreshToken(
            String email
    ){
        String issuedRefreshToken = Jwts.builder()
                .subject(jwtProperties.getPayload().getSubjectRefreshToken())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtProperties.getValidations().getRefresh()))
                .signWith(getSecretKey())
                .compact();
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .refreshToken(issuedRefreshToken)
                        .email(email)
                        .build()
        );
        return issuedRefreshToken;
    }

    private String issueAccessToken(
            String email,
            Role role
    ){
        return Jwts.builder()
                .subject(jwtProperties.getPayload().getSubjectAccessToken())
                .claim("role", role.name())
                .issuer(jwtProperties.getPayload().getIssuer())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtProperties.getValidations().getAccess()))
                .signWith(getSecretKey())
                .compact();
    }

    public String issueValidationToken(
            String email
    ){
        return Jwts.builder()
                .subject(EMAIL_VALIDATION_TOKEN_NAME)
                .issuer(jwtProperties.getPayload().getIssuer())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtEmailExpirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    public KeyPair issueKeyPair(
            String email,
            Role role
    ){
        return new KeyPair(
                issueAccessToken(email,role),
                issueRefreshToken(email)
        );
    }

    public boolean validate(String token){
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch(ExpiredJwtException e){
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch(MalformedJwtException e){
            throw new BusinessException(ErrorCode.ABNORMAL_TOKEN);
        } catch(JwtException e){
            throw new BusinessException(ErrorCode.ERROR_FROM_TOKEN);
        }
    }


    public Jws<Claims> parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
    }

    public TokenBody parseJwt(String token){
        Jws<Claims> claimsJws = parseClaims(token);
        return TokenBody.builder()
                .email(String.valueOf(claimsJws.getPayload().get("email")))
                .build();
    }

}
