package io.teabag.assetbox.user.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserInfoResponse {
    private Long id;
    private String name;
    private String nickname;
    private Integer postCount;
    private Integer totalLikes;
    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }
    public void setTotalLikes(Integer totalLikes) {
        this.totalLikes = totalLikes;
    }
    @QueryProjection
    @Builder
    public UserInfoResponse(
            Long id,
            String name,
            String nickname
    ) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
    }
}
