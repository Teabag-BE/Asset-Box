package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.AdminsUserDetailResponse;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public interface UserEmailRepository {
    User userSave(User user);
    boolean existsUserByEmail(String email);
    User findByEmailOrThrow(String email);
    boolean existsWhiteListByEmail(String email);
    EmailWhiteList emailWhiteListSave(EmailWhiteList emailWhiteList);
    Optional<User> findByEmail(String email);
    AdminsUserDetailResponse findUserByAdmin(String role, String q, PageRequest pageRequest);
}
