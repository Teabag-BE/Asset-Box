package io.teabag.assetbox.file.service.download;

public interface FileDownloadService {

    String getPresignedDownloadUrl(String fileName);
}
