package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReposiotry extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    default User findByEmailOrThrow(String email){
        return findByEmail(email).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    boolean existsUserByEmail(String email);
}
