package io.teabag.assetbox.util;

import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostUpdateRequest;
import io.teabag.assetbox.request.dto.RequestCreateRequest;

import java.time.LocalDateTime;
import java.util.List;

public class TestUtil {
    public static PostCreateRequest postCreateRequestOf(){
        return new PostCreateRequest(
                "제목",
                "내용",
                1L,
                List.of("spring", "jpa"),
                null
        );
    }



    public static PostUpdateRequest postUpdateRequestOf(){
        return new PostUpdateRequest(
                "수정 제목",
                "수정 내용",
                1L,
                List.of("spring", "jpa")
        );
    }


    public static RequestCreateRequest requestCreateRequestOf(){
        return new RequestCreateRequest(
                "요청 제목",
                "요청 내용",
                "CHARACTER",
                "LOW_POLY",
                "UNITY",
                LocalDateTime.now().plusDays(7),
                1L
        );
    }
}
