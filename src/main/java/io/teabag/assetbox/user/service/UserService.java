package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserEmailRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final FileService fileService;

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

    public KeyPair signIn(LoginRequest loginRequest) {

        User founded = userRepository.findByEmail(loginRequest.email()).orElseThrow(
                ()-> new BusinessException(ErrorCode.LOGIN_FAILED)
                        );

        PreConditions.validate(
                passwordEncoder.matches(loginRequest.password(), founded.getPassword()),
                ErrorCode.LOGIN_FAILED
        );

        PreConditions.validate(
                founded.getDeletedAt() == null,
                ErrorCode.USER_ALREADY_DELETED
        );

        return tokenProvider.issueKeyPair(founded.getEmail(), founded.getRole());
    }

    public CurrentUser loadCurrentUserByEmail(String email){
        return CurrentUser.from(
                userRepository.findByEmailOrThrow(email)
        );
    }

    public MyInfoResponse getMyInfo(String email){
        User user = userRepository.findByEmailOrThrow(email);
        if (user.getAvatarKey() == null) {
            return MyInfoResponse.from(
                    user,null
            );
        }
        return MyInfoResponse.from(
                user,
                fileService.getShowPresignedUrl(user.getAvatarKey())
        );
    }

    @Transactional
    public UserUpdateResponse saveAvatar(String email, MultipartFile file){
        User user = userRepository.findByEmailOrThrow(email);
        String avatarKey = fileService.uploadThumbnail(file, ThumbnailPurpose.AVATAR, user.getId());
        user.setAvatarKey(avatarKey);
        return UserUpdateResponse.from(
                user
        );
    }
}
