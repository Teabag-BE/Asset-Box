package io.teabag.assetbox.user.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.teabag.assetbox.common.dto.JwtProperties;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.common.constants.TokenType;
import io.teabag.assetbox.user.dto.TokenBody;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenProvider {
    private final JwtProperties jwtProperties;
    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecrets().getAppKey().getBytes());
    }

    private String issueRefreshToken(
            String email
    ){
        return Jwts.builder()
                .subject(jwtProperties.getPayload().getSubjectRefreshToken())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtProperties.getValidations().getRefresh()))
                .signWith(getSecretKey())
                .compact();
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
    public TokenBody parseJwt(String token, TokenType tokenType){
        Jws<Claims> claimsJws = parseClaims(token);
        return switch (tokenType){
            case TokenType.ACCESS_TOKEN -> TokenBody.builder()
                    .email(String.valueOf(claimsJws.getPayload().get("email")))
                    .role(Role.valueOf(claimsJws.getPayload().get("role").toString()))
                    .build();
            case TokenType.REFRESH_TOKEN -> TokenBody.builder()
                    .email(String.valueOf(claimsJws.getPayload().get("email")))
                    .build();
        };
    }

}
