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
    private FilePurpose purpose;

    @Column(nullable = false)
    private Long purposeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by",  nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private Long uploadOrder;


    @Column(length = 100)
    private String contentType;
    /*
    contentType은 브라우저나 S3, HTTP 응답에서 “이 파일을 어떻게 다룰지” 판단할 때 씁니다.
    이미지 파일을 다운로드할 때 Content-Type이 image/png면 브라우저가 이미지로 인식합니다.
    반대로 application/octet-stream이면 브라우저는 그냥 일반 바이너리 파일처럼 취급할 수 있습니다.
    예시
    image/png
    image/jpeg
    application/pdf
    application/octet-stream
    model/gltf-binary
     */



    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetFileType fileType;

    @Builder
    public File(String originalName, String savedName, String extension, Long sizeBytes, FilePurpose purpose, Long purposeId, User uploadedBy, Long uploadOrder, String contentType, AssetFileType fileType) {
        this.originalName = originalName;
        this.savedName = savedName;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.purpose = purpose;
        this.purposeId = purposeId;
        this.uploadedBy = uploadedBy;
        this.uploadOrder = uploadOrder;
        this.contentType = contentType;
        this.fileType = fileType;
    }
}
