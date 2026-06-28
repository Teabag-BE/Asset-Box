package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostTag;

import java.util.List;

public record PostInfo (
        Long id,
        String title,
        String content,
        Long authorId,
        Long categoryId,
        String thumbnailKey,
        String thumbnailUrl,
        List<PostFileInfo> files,
        List<String> tags,
        Long linkedRequestId
){
    //썸네일 없는 경우 포스트
    public static PostInfo from(Post post) {
        List<String> tags = post.getPostTags().stream()
                .map(postTag -> postTag.getTag().getName())
                .toList();

        return new PostInfo(post.getId(), post.getTitle(), post.getContent(), post.getAuthorId(),
                post.getCategoryId(), post.getThumbnailKey(), null, null, tags, post.getLinkedRequestId());
    }

    //썸네일 있는 경우 포스트
    public PostInfo setThumbnailUrl (String thumbnailUrl) {
        return new PostInfo(this.id(), this.title(), this.content(), this.authorId(),
                this.categoryId(), this.thumbnailKey(), thumbnailUrl, null, this.tags(),this.linkedRequestId());
    }

    //파일 정보를 세팅한 경우 포스트
    public PostInfo setfiles (List<PostFileInfo> files) {
        return new PostInfo(this.id(), this.title(), this.content(), this.authorId(),
                this.categoryId(), this.thumbnailKey(), this.thumbnailUrl(), files, this.tags(),this.linkedRequestId());
    }
}
