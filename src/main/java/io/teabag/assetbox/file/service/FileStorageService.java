package io.teabag.assetbox.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String save(MultipartFile file);
}
