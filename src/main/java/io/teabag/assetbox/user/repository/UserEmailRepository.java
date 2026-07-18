package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.email.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.SearchUserByAdminResponse;
import io.teabag.assetbox.user.dto.directory.SearchUserResponse;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public interface UserEmailRepository {
    User userSave(User user);
    boolean existsUserByEmail(String email);
    User findByEmailOrThrow(String email);
    boolean existsWhiteListByEmail(String email);
    EmailWhiteList findEmailWhiteListByEmailOrThrow(String email);
    EmailWhiteList emailWhiteListSave(EmailWhiteList emailWhiteList);
    Optional<User> findByEmail(String email);
    SearchUserByAdminResponse findUserByAdmin(String role, String q, PageRequest pageRequest);
    User findByIdOrThrow(Long id);
    SearchUserResponse findUser(String sortColumn, String sortType, String role, String q, PageRequest pageRequest);
}
