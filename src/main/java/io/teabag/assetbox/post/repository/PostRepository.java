package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByDeletedAtIsNull();

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
    default Post findByIdOrThrow(Long id){
        return findById(id).orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
