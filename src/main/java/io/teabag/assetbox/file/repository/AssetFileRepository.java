package io.teabag.assetbox.file.repository;

import io.teabag.assetbox.file.domain.AssetFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetFileRepository extends JpaRepository<AssetFile, Long> {
}
