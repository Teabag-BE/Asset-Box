package io.teabag.assetbox.email.domain;

import io.teabag.assetbox.email.constants.EmailStatus;
import io.teabag.assetbox.user.constants.Major;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailWhiteList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Major major;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private EmailStatus emailStatus;

    @Builder
    public EmailWhiteList(
            String name,
            Major major,
            String email
    ){
        this.name = name;
        this.major = major;
        this.email = email;
        emailStatus = EmailStatus.ENROLL;
    }

    public void switchVerified(){
        this.emailStatus = EmailStatus.VERIFIED;
    }
}
