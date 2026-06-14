package io.teabag.assetbox.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Asset Box",
                version = "0.0.1",
                description = "Asset Box API 명세"
        )
)
public class SwaggerConfig {

    SecurityScheme securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .components(
                      new Components().addSecuritySchemes("bearerAuth",securityScheme)
                ).addSecurityItem(securityRequirement);
    }

    @Bean
    public GroupedOpenApi userApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/users/**", "/api/oauth2/**" , "/api/admin/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi postApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/posts/**", "/api/admin/posts/**")
                .pathsToExclude("/api/posts/*/comments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi requestApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/requests/**")
                .pathsToExclude("/api/requests/*/comments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi commentApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/posts/*/comments/**","/api/requests/*/comments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi categoryApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/categories/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fileApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/files/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messageApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/messages/**")
                .build();
    }

    @Bean
    public GroupedOpenApi webSocketApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/user/queue/**")
                .build();
    }

    @Bean
    public GroupedOpenApi feedbackApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/feedback/**")
                .pathsToMatch("/api/admin/feedback/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi(){
        return GroupedOpenApi.builder()
                .pathsToMatch("/actuator/health")
                .pathsToMatch("/actuator/prometheus")
                .build();
    }
}
