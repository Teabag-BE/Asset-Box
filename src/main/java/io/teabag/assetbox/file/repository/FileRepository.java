package io.teabag.assetbox.file.repository;

import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByPurposeAndPurposeId(FilePurpose purpose, Long purposeId);

    List<File> findByPurposeAndPurposeIdOrderByUploadOrderAsc(FilePurpose purpose, Long purposeId);

    @Query("""
	select coalesce(sum(f.sizeBytes), 0)
	from File f
	where f.purpose = :purpose
	  and f.purposeId = :purposeId
	  and f.deletedAt is null
""")
    long sumSizeBytesByPurposeAndPurposeId(
        @Param("purpose") FilePurpose purpose,
        @Param("purposeId") Long purposeId
    );

    List<File> findAllByIdIn(Collection<Long> ids);
}
