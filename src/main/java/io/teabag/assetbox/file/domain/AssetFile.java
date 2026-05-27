package io.teabag.assetbox.file.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_files")
public class AssetFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String originalName;

    @Column(nullable = false, length = 500)
    private String storedPath;

    @Column(nullable = false, length = 30)
    private String extension;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilePurpose purpose;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(nullable = false)
    private boolean deleted = false;

    protected AssetFile() {
    }

    public AssetFile(String originalName, String storedPath, String extension, long sizeBytes, FilePurpose purpose, Long ownerId, Long uploadedBy) {
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.purpose = purpose;
        this.ownerId = ownerId;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getExtension() { return extension; }
    public long getSizeBytes() { return sizeBytes; }
    public FilePurpose getPurpose() { return purpose; }
    public Long getOwnerId() { return ownerId; }
    public Long getUploadedBy() { return uploadedBy; }
}
