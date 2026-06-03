package io.teabag.assetbox.common.filter;

import io.teabag.assetbox.common.constants.TokenType;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.AccessTokenBody;
import io.teabag.assetbox.user.service.TokenProvider;
import io.teabag.assetbox.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final UserService userService;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String extractedToken = extractToken(request);

        if (extractedToken == null){
            filterChain.doFilter(request,response);
            return;
        }

        if (tokenProvider.validate(extractedToken)) {
            AccessTokenBody accessTokenBody = tokenProvider.parseJwt(extractedToken, TokenType.ACCESS_TOKEN);
            CurrentUser currentUser = userService.loadCurrentUserByEmail(accessTokenBody.email());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    currentUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request,response);
    }
    public String extractToken(HttpServletRequest request){
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(bearerToken != null && bearerToken.startsWith("Bearer ")) return bearerToken.substring(7);
        return null;
    }
}
