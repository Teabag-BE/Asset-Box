package io.teabag.assetbox.request.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "request_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestPost extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 60)
    private String assetType;

    @Column(length = 60)
    private String preferredStyle;

    @Column(length = 60)
    private String engine;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.REQUESTED;

    // FKs
    @Column(nullable = false)
    private Long requesterId;

    private Long assigneeId;

    private Long linkedPostId;

//    M1에서는 teamId 보류
//    private Long teamId;

    @Builder
    public RequestPost(String title,
                       String content,
                       String assetType,
                       String preferredStyle,
                       String engine,
                       LocalDateTime deadline,
                       Long requesterId
    ) {
        this.title = title;
        this.content = content;
        this.assetType = assetType;
        this.preferredStyle = preferredStyle;
        this.engine = engine;
        this.deadline = deadline;
        this.requesterId = requesterId;

        // 생성규칙 : 생성시 REQUESTED 상태로 시작.
        this.status = RequestStatus.REQUESTED;
    }

    public void softDelete() { setDeletedAt(); }

}
