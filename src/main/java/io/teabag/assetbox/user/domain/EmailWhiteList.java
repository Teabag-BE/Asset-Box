package io.teabag.assetbox.user.domain;

import io.teabag.assetbox.user.repository.EmailWhiteListRepository;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailWhiteList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Builder
    public EmailWhiteList(
            String email
    ){
        this.email = email;
    }
}
