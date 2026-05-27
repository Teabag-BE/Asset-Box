package io.teabag.assetbox.post.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private Long categoryId;

    private Long thumbnailFileId;
    private Long teamId;
    private Long linkedRequestId;

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    protected Post() {
    }
}
