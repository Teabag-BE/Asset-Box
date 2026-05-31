package io.teabag.assetbox;

import io.teabag.assetbox.post.dto.PostCreateRequest;

import java.util.List;

public class TestUtil {
    public static PostCreateRequest postCreateRequestOf(){
        return new PostCreateRequest(
                "제목",
                "내용",
                1L,
                1L,
                List.of("spring", "jpa"),
                null
        );
    }
}
