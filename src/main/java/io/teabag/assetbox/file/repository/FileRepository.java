package io.teabag.assetbox.file.repository;

import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByPurposeAndPurposeId(FilePurpose purpose, Long purposeId);

    List<File> findByPurposeAndPurposeIdOrderByUploadOrderAsc(FilePurpose purpose, Long purposeId);

    List<File> findAllByIdIn(Collection<Long> ids);
}
