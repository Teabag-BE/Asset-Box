package io.teabag.assetbox.user.domain;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.user.constants.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 50)
    private String major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 100)
    private String providerSubject;

    private Long teamId;

    @Column(length = 255)
    private String avatarPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    protected User() {
    }

    public User(String email, String password, String name, String nickname, String major) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.major = major;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getMajor() { return major; }
    public AuthProvider getProvider() { return provider; }
    public Role getRole() { return role; }
    public String getAvatarPath() { return avatarPath; }
}
