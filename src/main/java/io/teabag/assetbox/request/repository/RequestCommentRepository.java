package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.request.domain.RequestComment;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {

    Slice<RequestComment> findAllByRequestIdAndDeletedAtIsNull(Long requestId, Pageable pageable);

    Optional<RequestComment> findByIdAndDeletedAtIsNull(Long id);

    default RequestComment findByIdOrThrow(Long id) {
        return findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
