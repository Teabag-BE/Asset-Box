package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.post.domain.Post;

public record PostInfo (
        Long id,
        String title,
        String content,
        Long authorId,
        Long categoryId,
        String thumbnailKey,
        String thumbnailUrl,
        Long linkedRequestId
){
    //썸네일 없는 경우 포스트
    public static PostInfo from(Post post) {
        return new PostInfo(post.getId(), post.getTitle(), post.getContent(), post.getAuthorId(),
                post.getCategoryId(), post.getThumbnailKey(), null, post.getLinkedRequestId());
    }

    //썸네일 있는 경우 포스트
    public PostInfo setThumbnailUrl (String thumbnailUrl) {
        return new PostInfo(this.id(), this.title(), this.content(), this.authorId(),
                this.categoryId(), this.thumbnailKey(), thumbnailUrl, this.linkedRequestId());
    }
}
