package io.teabag.assetbox.post.dto;

// 좋아요 토글 결과: 반영 후 총 좋아요 수와 현재 사용자의 좋아요 상태
public record LikeResponse(long likeCount, boolean liked) {
}
