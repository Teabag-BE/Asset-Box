package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Slice<Post> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
    default Post findByIdOrThrow(Long id){
        return findById(id).orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    @Query("""
        SELECT count(*)
            FROM Post p
            WHERE p.authorId = :userId
    """)
    Integer getCountByRequesterId(Long userId);
}
