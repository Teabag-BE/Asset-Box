package io.teabag.assetbox.util;

import io.teabag.assetbox.post.domain.Post;

public class PostUtil {

    public static Post create(
            Long authorId,
            Long categoryId,
            Long linkedRequestId
    ){
        return Post.builder()
                .title("Post 제목입니다.")
                .content("Post 내용입니다.")
                .categoryId(categoryId)
                .authorId(authorId)
                .linkedRequestId(linkedRequestId)
                .build();
    }
}
