package io.teabag.assetbox.file.domain;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String originalName;

    @Column(nullable = false, length = 500)
    private String savedUrl;

    @Column(nullable = false, length = 30)
    private String extension;

    @Column(nullable = false)
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilePurpose domainType;

    @Column(nullable = false)
    private Long domainId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Builder
    public File(String originalName, String savedUrl, String extension, Long sizeBytes, FilePurpose domainType, Long domainId, User uploadedBy) {
        this.originalName = originalName;
        this.savedUrl = savedUrl;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.domainType = domainType;
        this.domainId = domainId;
        this.uploadedBy = uploadedBy;
    }
}
