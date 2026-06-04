package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;

import java.util.Optional;

public interface UserEmailRepository {
    User userSave(User user);
    boolean existsUserByEmail(String email);
    User findByEmailOrThrow(String email);
    boolean existsWhiteListByEmail(String email);
    EmailWhiteList emailWhiteListSave(EmailWhiteList emailWhiteList);
    Optional<User> findByEmail(String email);
}
