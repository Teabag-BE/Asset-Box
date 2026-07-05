package io.teabag.assetbox.common.config;

import io.teabag.assetbox.common.eventhandler.OauthSuccessHandler;
import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.user.constants.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OauthSuccessHandler oauthSuccessHandler;
    private final JwtFilter jwtFilter;

    @Value("${custom.baseUrl}")
    public String BASE_URL;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(
                        oauth -> oauth.successHandler(oauthSuccessHandler)
                )
                // REST API 인증/인가 실패는 OAuth 로그인 페이지로 302 redirect하지 않고
                // 상태 코드만 반환한다. HTTPS 페이지에서 http://.../login redirect가
                // Mixed Content로 차단되는 문제를 막기 위한 설정이다.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(HttpStatus.UNAUTHORIZED.value());
                                return;
                            }
                            response.sendRedirect(BASE_URL + "/login");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(HttpStatus.FORBIDDEN.value());
                                return;
                            }
                            response.sendRedirect(BASE_URL + "/login");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 개발 환경에서 필요한거 추후 운영에서 빼야됨
                        .requestMatchers("/h2-console/**", "/api/files/**" , "/api/actuator/**", "/v3/**" ).permitAll()

                        // Preflight Request 허용
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()

                        // 로그인 / 회원가입은 익명 사용자만 가능
                        .requestMatchers(HttpMethod.GET, EndPoints.GET_ANONYMOUS).anonymous()
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_ANONYMOUS).anonymous()

                        .requestMatchers(HttpMethod.GET, EndPoints.GET_ADMIN_AUTHENTICATED).hasAnyRole(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, EndPoints.GET_AUTHENTICATED).authenticated()

                        .requestMatchers(HttpMethod.POST, EndPoints.POST_PERMIT_ALL).permitAll()
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_ADMIN_AUTHENTICATED).hasAnyRole(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_AUTHENTICATED).authenticated()

                        .requestMatchers(HttpMethod.PUT, EndPoints.PUT_ADMIN_AUTHENTICATED).hasAnyRole(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, EndPoints.PUT_AUTHENTICATED).authenticated()

                        .requestMatchers(HttpMethod.PATCH, EndPoints.PATCH_ADMIN_AUTHENTICATED).hasAnyRole(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, EndPoints.PATCH_AUTHENTICATED).authenticated()

                        .requestMatchers(HttpMethod.DELETE, EndPoints.DELETE_ADMIN_AUTHENTICATED).hasAnyRole(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, EndPoints.DELETE_AUTHENTICATED).authenticated()

                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


     // CORS 관련 설정
    @Bean
    public WebMvcConfigurer corsConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(BASE_URL)
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .exposedHeaders("Authorization")
                        .maxAge(3600);
            }
        };
    }

    static public class EndPoints {

        public static final String[] GET_ANONYMOUS = { "/api/users/login", "/api/users/oauth2/authorization/**", "/oauth2/authorization/**" } ;
        public static final String[] GET_AUTHENTICATED = { "/api/users/**", "/api/posts/**", "/api/requests/**",
                "/api/categories/**", "/api/files/**", "/api/messages/**"  } ;
        public static final String[] GET_ADMIN_AUTHENTICATED = { "/api/admin/**"};

        public static final String[] POST_ANONYMOUS = { "/api/users/login", "/api/users/signup"} ;
        public static final String[] POST_PERMIT_ALL = { "/api/users/refresh"  };
        public static final String[] POST_AUTHENTICATED = { "/api/users/**", "/api/posts/**", "/api/requests/**",
                 "/api/files/**", "/api/messages/**", "/api/feedback/**"  };
        public static final String[] POST_ADMIN_AUTHENTICATED = { "/api/categories/**", "/api/admin/**"};

        public static final String[] PUT_AUTHENTICATED = { "/api/users/**", "/api/posts/**", "/api/requests/**",
                 "/api/files/**", "/api/messages/**", "/api/feedback/**"  };
        public static final String[] PUT_ADMIN_AUTHENTICATED = { "/api/admin/**"} ;

        public static final String[] PATCH_AUTHENTICATED = { "/api/users/**", "/api/posts/**", "/api/requests/**",
                "/api/categories/**", "/api/files/**", "/api/messages/**", "/api/feedback/**"  };
        public static final String[] PATCH_ADMIN_AUTHENTICATED = { "/api/categories/**", "/api/admin/**"} ;

        public static final String[] DELETE_AUTHENTICATED = { "/api/users/**", "/api/posts/**", "/api/requests/**",
                "/api/files/**", "/api/messages/**", "/api/feedback/**"  };
        public static final String[] DELETE_ADMIN_AUTHENTICATED = { "/api/admin/**"} ;

    }
}
