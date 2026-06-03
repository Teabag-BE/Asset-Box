package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.teabag.assetbox.user.repository.UserReposiotry;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserEmailRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserCreateResponse signup(SignupRequest request) {

        PreConditions.validate(
                !userRepository.existsUserByEmail(request.email()),
                ErrorCode.USER_EMAIL_DUPLICATED
        );

        PreConditions.validate(
                userRepository.existsWhiteListByEmail(request.email()),
                ErrorCode.USER_EMAIL_NOT_WHITELISTED
        );

        return UserCreateResponse.from(
                userRepository.userSave(
                    User.builder()
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .name(request.name())
                            .major(Major.valueOf(request.major()))
                            .nickname(request.nickname())
                            .build()
            )
        );
    }

    public CurrentUser loadCurrentUserByEmail(String email){
        return CurrentUser.from(
                userRepository.findByEmailOrThrow(email)
        );
    }
}
