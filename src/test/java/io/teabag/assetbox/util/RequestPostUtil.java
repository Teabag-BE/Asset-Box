package io.teabag.assetbox.util;

import io.teabag.assetbox.request.domain.RequestPost;

public class RequestPostUtil {
    public static RequestPost create(Long requesterId){
        return RequestPost.builder()
                .title("요청게시글 제목")
                .content("요청게시글 내용")
                .assetType("자원 형식")
                .preferredStyle("선호 스타일")
                .engine("엔진")
                .requesterId(requesterId)
                .build();
    }
}
