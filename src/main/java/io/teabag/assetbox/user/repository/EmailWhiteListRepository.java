package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.user.domain.EmailWhiteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailWhiteListRepository extends JpaRepository<EmailWhiteList,Long> {
    boolean existsByEmail(String email);
}
