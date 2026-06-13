package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.dto.KeyPair;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.AdminsUserDetailResponse;
import io.teabag.assetbox.user.dto.LoginRequest;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserCreateResponse;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserEmailRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and #userEmail == authentication.principal.email")
    public AdminsUserDetailResponse getUserDetailsByAdmin(
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
}
