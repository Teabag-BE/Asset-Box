package io.teabag.assetbox.user.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserInfoResponse {
    private Long id;
    private String name;
    private String nickname;
    private String imageUrl;
    private Long postCount;
    private Long totalLikes;
    public void setImageUrl(String url){this.imageUrl = url; }
    @QueryProjection
    @Builder
    public UserInfoResponse(
            Long id,
            String name,
            String nickname,
            Long postCount,
            Long totalLikes
    ) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
        this.postCount = postCount;
        this.totalLikes = totalLikes;
    }
}
