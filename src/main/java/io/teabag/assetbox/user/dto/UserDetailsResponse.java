package io.teabag.assetbox.user.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserDetailsResponse{
    private Long id;
    private String email;
    private String nickname;
    private String major;
    private String provider;
    private String role;
    private boolean isOauthLinked;
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
    public UserDetailsResponse(
            Long id,
            String email,
            String nickname,
            Major major,
            String provider,
            Role role,
            boolean isOauthLinked
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.major = major.name();
        this.provider = provider;
        this.role = role.name();
        this.isOauthLinked = isOauthLinked;
    }
}

