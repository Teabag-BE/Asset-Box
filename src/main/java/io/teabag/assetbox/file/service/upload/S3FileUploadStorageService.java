package io.teabag.assetbox.file.service.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class S3FileUploadStorageService {

    public String save(MultipartFile file) {
        return UUID.randomUUID() + "-" + file.getOriginalFilename();
    }
}
