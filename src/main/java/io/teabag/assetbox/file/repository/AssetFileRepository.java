package io.teabag.assetbox.file.repository;

import io.teabag.assetbox.file.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetFileRepository extends JpaRepository<File, Long> {
}
