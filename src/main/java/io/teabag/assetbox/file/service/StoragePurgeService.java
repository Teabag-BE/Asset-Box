package io.teabag.assetbox.file.service;

import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoragePurgeService {

    private final FileRepository fileRepository;
    private final PostRepository postRepository;
    private final S3FileStorageService s3FileStorageService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredStorageObjects() {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredFiles(now);
        purgeExpiredThumbnails(now);
    }

    private void purgeExpiredFiles(LocalDateTime now) {
        List<File> files = fileRepository.findByDeletedAtIsNotNullAndPurgeAtLessThanEqualAndStorageDeletedAtIsNull(now);

        for (File file : files) {
            try {
                s3FileStorageService.delete(file.getS3Key());
                file.markStorageDeleted();
            } catch (Exception e) {
                log.warn("Failed to purge storage object. fileId = {}, s3Key = {}", file.getId(), file.getS3Key(), e);
            }
        }
    }

    private void purgeExpiredThumbnails(LocalDateTime now) {
        List<Post> posts = postRepository
            .findByThumbnailKeyIsNotNullAndThumbnailPurgeAtLessThanEqualAndThumbnailStorageDeletedAtIsNull(now);

        for (Post post : posts) {
            try {
                s3FileStorageService.delete(post.getThumbnailKey());
                post.markThumbnailStorageDeleted();
            } catch (Exception e) {
                log.warn("Failed to purge post thumbnail. postId = {}, thumbnailKey = {}", post.getId(), post.getThumbnailKey(), e);
            }
        }
    }
}
