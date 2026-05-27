package io.teabag.assetbox.user.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SignupRequest;
import io.teabag.assetbox.user.dto.UserResponse;
import io.teabag.assetbox.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        User user = new User(request.email(), request.password(), request.name(), request.nickname(), request.major());
        return UserResponse.from(userRepository.save(user));
    }

    public User requireExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
