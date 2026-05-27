package io.teabag.assetbox.file.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Override
    public String save(MultipartFile file) {
        return UUID.randomUUID() + "-" + file.getOriginalFilename();
    }
}
