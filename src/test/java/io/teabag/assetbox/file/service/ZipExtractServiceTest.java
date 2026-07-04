package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipExtractServiceTest {

    private final ZipExtractService zipExtractService = new ZipExtractService();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ZIP 내부에 FBX가 없으면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenFbxDoesNotExist() throws Exception {
        // given
        Path zipFile = createZipFile("no-model.zip", new ZipTestEntry("basecolor.png", "texture".getBytes()));

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부에 FBX가 2개 이상이면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenMultipleFbxExist() throws Exception {
        // given
        Path zipFile = createZipFile(
            "multiple-models.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("sub/other.fbx", "model2".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부에 허용되지 않은 확장자가 있으면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenExtensionIsNotAllowed() throws Exception {
        // given
        Path zipFile = createZipFile(
            "invalid-extension.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("script.exe", "bad".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부에 Zip Slip 경로가 있으면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenZipSlipPathExists() throws Exception {
        // given
        Path zipFile = createZipFile(
            "zip-slip.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("../evil.png", "evil".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("정상 ZIP이면 FBX와 텍스처 파일을 분류한다")
    void extractAssetZip_returnsModelAndTextures() throws Exception {
        // given
        Path zipFile = createZipFile(
            "asset.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("textures/basecolor.png", "texture".getBytes()),
            new ZipTestEntry("__MACOSX/asset", "ignored".getBytes()),
            new ZipTestEntry(".DS_Store", "ignored".getBytes())
        );

        // when
        ZipExtractService.ZipExtractResult result = zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract"));

        // then
        assertThat(result.model().fileType()).isEqualTo(AssetFileType.MODEL);
        assertThat(result.model().originalName()).isEqualTo("model.fbx");
        assertThat(result.textures()).hasSize(1);
        assertThat(result.textures().getFirst().fileType()).isEqualTo(AssetFileType.TEXTURE);
        assertThat(result.textures().getFirst().originalName()).isEqualTo("basecolor.png");
        assertThat(Files.exists(result.model().path())).isTrue();
        assertThat(Files.exists(result.textures().getFirst().path())).isTrue();
    }

    private Path createZipFile(String filename, ZipTestEntry... entries) throws Exception {
        Path zipFile = tempDir.resolve(filename);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (ZipTestEntry entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.name()));
                zipOutputStream.write(entry.content());
                zipOutputStream.closeEntry();
            }
        }

        Files.write(zipFile, outputStream.toByteArray());
        return zipFile;
    }

    private record ZipTestEntry(String name, byte[] content) {
    }
}
