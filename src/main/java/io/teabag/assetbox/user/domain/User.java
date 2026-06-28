package io.teabag.assetbox.user.domain;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.dto.SignupRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(length = 50)
    public String publicEmail;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Major major;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String provider;

    @Setter
    @Column(length = 500)
    private String avatarKey;

    @Column(columnDefinition = "INT DEFAULT 0")
    private boolean isOauthLinked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role ;

    @Builder
    public User(
            String email,
            String password,
            String name,
            String nickname,
            Major major,
            String avatarKey
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.major = major;
        this.avatarKey = avatarKey;
        this.publicEmail = email;

        isOauthLinked = false;
        role = Role.USER;
        provider = extractProvider(email);
    }

    public void updateRole(Role role){
        this.role = role;
    }

    public String extractProvider(String email){
        String[] s1 = email.split("@");
        String[] s2 = s1[1].split("\\.");
        return s2[0];
    }

    public void updateProfile(String nickname, Major major, String publicEmail, String description){
        if (nickname != null){
            this.nickname=nickname;
        }
        if (major != null){
            this.major=major;
        }
        if (publicEmail != null){
            this.publicEmail=publicEmail;
        }
        if (description!= null){
            this.description=description;
        }
    }

}
