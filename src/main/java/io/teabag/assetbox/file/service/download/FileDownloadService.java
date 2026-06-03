package io.teabag.assetbox.file.service.download;

public interface FileDownloadService {

    String getDownloadPresignedUrl(String fileName);
}
