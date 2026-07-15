package io.teabag.assetbox.request.domain;

import io.teabag.assetbox.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "request_comments")
public class RequestComment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private Long authorId;

    // 게시글 댓글과 동일하게 작성 시점 닉네임을 저장(조회 시 N+1 방지).
    @Column(nullable = false)
    private String authorNickname;

    private Long parentId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    public RequestComment(Long id, Long requestId, Long authorId, String authorNickname, Long parentId, String content) {
        this.id = id;
        this.requestId = requestId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.parentId = parentId;
        this.content = content;
    }

    protected RequestComment() {}

    // soft delete: BaseEntity.deletedAt 을 채운다(프론트는 deletedAt 유무로 삭제 판정).
    public void softDelete() {
        setDeletedAt();
        this.deleted = true;
    }
}
