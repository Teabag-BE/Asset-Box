package io.teabag.assetbox.post.domain;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.tag.domain.Tag;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Setter
    @Column(length = 500)
    private String thumbnailKey;

    private LocalDateTime thumbnailPurgeAt;

    private LocalDateTime thumbnailStorageDeletedAt;

    private Long linkedRequestId;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private long total_file_size = 0;

    private String image_resolution;

    private long polygon = 0;

    @Builder

    public Post(
            String title,
            String content,
            Long authorId,
            Long categoryId,
            Long linkedRequestId
    ) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.categoryId = categoryId;
        this.linkedRequestId = linkedRequestId;
        this.viewCount = 0;
        this.likeCount = 0;
        this.total_file_size = 0;
    }

    public void addTag(Tag tag) {
        PostTag postTag = new PostTag(this, tag);
        postTags.add(postTag);
    }

    public void softDelete() {
        setDeletedAt();
    }

    public void markThumbnailDeletedWithRetention(Duration retention) {
        if (thumbnailKey == null) {
            return;
        }

        this.thumbnailPurgeAt = LocalDateTime.now().plus(retention);
    }

    public void markThumbnailStorageDeleted() {
        this.thumbnailStorageDeletedAt = LocalDateTime.now();
    }

    public void clearTags() {
        this.postTags.clear();
    }

    public void update(
            String title,
            String content,
            Long categoryId
    ) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
    }

    // 조회 시 조회수 증가
    public void incrementView() {
        this.viewCount++;
    }

    // 좋아요 토글에 따른 카운트 증감 (음수 방지)
    public void addLike() {
        this.likeCount++;
    }

    public void removeLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

}
