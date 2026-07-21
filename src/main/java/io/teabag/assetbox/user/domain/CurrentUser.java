package io.teabag.assetbox.user.domain;


import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CurrentUser implements OAuth2User {
    private Long id;
    private String email;
    private String name;
    private Role role;
    private Major major;
    private Map<String,Object> attributes;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }
    @Builder
    private CurrentUser(
            Long id,
            Role role,
            String email,
            String name,
            Major major,
            Map<String, Object> attributes
    ){
        this.id = id;
        this.role = role;
        this.email = email;
        this.name = name;
        this.major = major;
        this.attributes = attributes;
    }

    public static CurrentUser from(User user){
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return CurrentUser.builder()
                .id(user.getId())
                .role(user.getRole())
                .email(user.getEmail())
                .name(user.getName())
                .major(user.getMajor())
                .build();
    }

}
