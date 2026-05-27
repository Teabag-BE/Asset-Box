package io.teabag.assetbox.comment.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
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
    protected Comment() {}
}
