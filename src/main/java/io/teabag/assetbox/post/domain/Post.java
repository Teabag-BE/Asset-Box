package io.teabag.assetbox.post.domain;

import io.teabag.assetbox.common.BaseEntity;
import io.teabag.assetbox.tag.domain.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "posts")
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

    private Long thumbnailFileId;
    //private Long teamId;
    private Long linkedRequestId;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private long total_file_size = 0;

    private String image_resolution;

    private long polygon = 0;

    @Builder
    protected Post(
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
        this.deleted = false;
        this.total_file_size = 0;
    }

    public void addTag(Tag tag) {
        PostTag postTag = new PostTag(this, tag);
        postTags.add(postTag);
    }

}
