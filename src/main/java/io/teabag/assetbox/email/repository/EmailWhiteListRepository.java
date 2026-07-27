package io.teabag.assetbox.email.repository;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailWhiteListRepository extends JpaRepository<EmailWhiteList,Long> {
    Boolean existsByEmail(String email);

    Optional<EmailWhiteList> findByEmail(String email);

    default EmailWhiteList findByEmailOrThrow(String email){
        return findByEmail(email).orElseThrow(
                ()-> new BusinessException(ErrorCode.EMAIL_NOT_ON_WHITELIST)
        );
    }

    void deleteByEmail(String email);
}
