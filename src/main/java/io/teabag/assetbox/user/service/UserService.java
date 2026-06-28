package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SearchUserByAdminResponse;
import io.teabag.assetbox.user.dto.LoginRequest;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.dto.directory.SearchUserResponse;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public MyInfoResponse saveAvatar(String email, MultipartFile file){
        User user = userRepository.findByEmailOrThrow(email);
        String avatarKey = fileService.uploadThumbnail(file, ThumbnailPurpose.AVATAR, user.getId());
        user.setAvatarKey(avatarKey);

        return MyInfoResponse.from(
                user, fileService.getShowPresignedUrl(user.getAvatarKey())
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and #userEmail == authentication.principal.email")
    public SearchUserByAdminResponse getUserDetailsByAdmin(
            String userEmail,
            PageRequest pageRequest,
            String q,
            String role
    ){
        User founded = userRepository.findByEmailOrThrow(userEmail);

        PreConditions.validate(
                founded.getRole().equals(Role.ADMIN) || founded.getRole().equals(Role.SUPER_ADMIN),
                ErrorCode.ACCOUNT_NOT_ADMIN
        );

        if (Strings.isNotBlank(role)){
            try {
                Role.valueOf(role.toUpperCase());
                role = role.toUpperCase();
            } catch (Exception e){
                throw new BusinessException(ErrorCode.INPUT_NOT_VALID, "역할을 잘못 입력했습니다.");
            }
        }

        return userRepository.findUserByAdmin(
                role,
                q,
                pageRequest
        );
    }

    @PreAuthorize("#userEmail == authentication.principal.email and isAuthenticated()")
    public SearchUserByAdminResponse searchUserDetails(
            PageRequest pageRequest,
            String q,
            String role
    ){

        if (Strings.isNotBlank(role)){
            try {
                Role.valueOf(role.toUpperCase());
                role = role.toUpperCase();
            } catch (Exception e){
                throw new BusinessException(ErrorCode.INPUT_NOT_VALID, "역할을 잘못 입력했습니다.");
            }
        }

        return userRepository.findUserByAdmin(
                role,
                q,
                pageRequest
        );
    }


    @PreAuthorize("hasRole('SUPER_ADMIN') and #adminEmail == authentication.principal.email")
    public void switchRole(
            Long subjectId,
            String adminEmail,
            Role role
    ){
        User foundedAdmin = userRepository.findByEmailOrThrow(adminEmail);

        PreConditions.validate(
                foundedAdmin.getRole().equals(Role.SUPER_ADMIN),
                    ErrorCode.ACCOUNT_NOT_SUPER_ADMIN
        );

        User foundedUser = userRepository.findByIdOrThrow(subjectId);

        PreConditions.validate(
                !foundedUser.getRole().equals(role),
                ErrorCode.CAN_NOT_SWITCH_TO_SAME_ROLE
        );

        PreConditions.validate(
                !foundedUser.getEmail().equals(adminEmail),
                ErrorCode.FORBIDDEN_SELF_ROLE_CHANGE
        );

        foundedUser.updateRole(role);
    }

    public UserProfileResponse getUserProfile(Long id, Role requesterRole){
        User targetUser = userRepository.findByIdOrThrow(id);

        String maskedEmail = (requesterRole == Role.ADMIN || requesterRole == Role.SUPER_ADMIN)
                ? targetUser.getEmail()
                : null;

        String avatarUrl = null;
        if (targetUser.getAvatarKey() != null) {
            avatarUrl = fileService.getShowPresignedUrl(targetUser.getAvatarKey());
        }
        return UserProfileResponse.from(targetUser, maskedEmail, avatarUrl);
    }

    @Transactional
    public MyInfoResponse updateMyInfo(String email, UserUpdateRequest request){
        User user = userRepository.findByEmailOrThrow(email);

        Major targetMajor = null;
        if (request.major() != null){
            targetMajor=Major.valueOf(request.major());
        }

        user.updateProfile(
                request.nickname(),
                targetMajor,
                request.publicEmail(),
                request.description()
        );

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



    @PreAuthorize("isAuthenticated()")
    public SearchUserResponse getUserInfoDetail(
            PageRequest pageRequest,
            String sortColumn,
            String sortType,
            String q,
            String major
    ){
        SearchUserResponse founded = userRepository.findUser(
                sortColumn,
                sortType,
                major,
                q,
                pageRequest
        );

        founded.items().forEach(
                (info)->{
                    User foundedUser = userRepository.findByIdOrThrow(info.getId());
                    if(foundedUser.getAvatarKey() != null){
                        info.setImageUrl(
                                fileService.getShowPresignedUrl(
                                foundedUser.getAvatarKey()
                            )
                        );
                    }
                }
        );

        return founded;
    }


    public User currentUserToUser(CurrentUser currentUser){
        return userRepository.findByIdOrThrow(currentUser.getId());
    }
}
