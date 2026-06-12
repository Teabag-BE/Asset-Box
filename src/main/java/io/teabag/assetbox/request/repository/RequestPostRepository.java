package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.request.domain.RequestPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestPostRepository extends JpaRepository<RequestPost, Long> {

    Slice<RequestPost> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<RequestPost> findByIdAndDeletedAtIsNull(Long id);

    default RequestPost findByIdOrThrow(Long id) {
        return findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
    }
}
