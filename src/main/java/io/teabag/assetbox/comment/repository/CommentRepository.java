package io.teabag.assetbox.comment.repository;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Slice<Comment> findAllByPostIdAndDeletedAtIsNull(Long postId, Pageable pageable);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
    default Comment findByIdOrThrow(Long id){
        return findByIdAndDeletedAtIsNull(id).orElseThrow(()-> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
