package io.teabag.assetbox.file.service.download;

import io.teabag.assetbox.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileRepository fileRepository;
    private final S3FileDownloadStorageService fileStorageService;

    @Override
    public String getPresignedDownloadUrl(String fileName) {
        return fileStorageService.createPresignedUrl(fileName);
    }
}
