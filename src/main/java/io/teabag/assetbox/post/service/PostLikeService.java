package io.teabag.assetbox.post.service;

import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostLike;
import io.teabag.assetbox.post.dto.LikeResponse;
import io.teabag.assetbox.post.repository.PostLikeRepository;
import io.teabag.assetbox.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    // 좋아요 토글: 이미 눌렀으면 취소, 아니면 추가. Post.likeCount 를 함께 동기화한다.
    @Transactional
    public LikeResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findByIdOrThrow(postId);
        Optional<PostLike> existing = postLikeRepository.findByUserIdAndPostId(userId, postId);

        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.removeLike();
            liked = false;
        } else {
            postLikeRepository.save(PostLike.builder().userId(userId).postId(postId).build());
            post.addLike();
            liked = true;
        }
        return new LikeResponse(post.getLikeCount(), liked);
    }

    // 현재 사용자가 이 글에 좋아요를 눌렀는지 (비로그인은 false)
    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
}
