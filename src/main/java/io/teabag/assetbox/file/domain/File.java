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
    private String savedName;

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
    @JoinColumn(name = "uploaded_by",  nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private Long uploadOrder;

    @Column(length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetFileType fileType;

    @Builder
    public File(String originalName, String savedName, String extension, Long sizeBytes, FilePurpose domainType, Long domainId, User uploadedBy, Long uploadOrder, String contentType, AssetFileType fileType) {
        this.originalName = originalName;
        this.savedName = savedName;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.domainType = domainType;
        this.domainId = domainId;
        this.uploadedBy = uploadedBy;
        this.uploadOrder = uploadOrder;
        this.contentType = contentType;
        this.fileType = fileType;
    }
}
