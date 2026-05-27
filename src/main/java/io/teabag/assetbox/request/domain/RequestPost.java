package io.teabag.assetbox.request.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "request_posts")
public class RequestPost extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    private String assetType;
    private String preferredStyle;
    private String engine;
    private LocalDate deadline;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.REQUESTED;
    @Column(nullable = false)
    private Long requesterId;
    private Long assigneeId;
    private Long linkedPostId;
    private Long teamId;
    @Column(nullable = false)
    private boolean deleted = false;
    protected RequestPost() {}
}
