package io.teabag.assetbox.util;

import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SignupRequest;

public class UserUtil {

    public static User createUser(
            String email,
            String password
    ){
        return User.builder()
                .email(email)
                .password(password)
                .name("이정수")
                .nickname("정수리")
                .major(Major.BACK_END)
                .build();
    }

    public static User createUser(
            String email,
            String password,
            String name
    ){
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .nickname("정수리")
                .major(Major.BACK_END)
                .build();
    }

    public static SignupRequest createUserCreateRequest(
            String email,
            String password,
            String nickName
    ){
        return new SignupRequest(
                email,
                password,
                "이정수",
                nickName,
                Major.BACK_END.toString()
        );
    }
}
