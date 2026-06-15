package io.teabag.assetbox.comment.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long postId;
    @Column(nullable = false)
    private Long authorId;
    private Long parentId;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    protected Comment(
            Long postId,
            Long authorId,
            Long parentId,
            String content
    ) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.deleted = false;
    }

    public void softDelete() {
        this.deleted = true;
        setDeletedAt();
    }
}
