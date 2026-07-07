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
    @DisplayName("ZIP 내부에 모델 파일이 없으면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenModelDoesNotExist() throws Exception {
        // given
        Path zipFile = createZipFile("no-model.zip", new ZipTestEntry("basecolor.png", "texture".getBytes()));

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부에 모델 파일이 2개 이상이면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenMultipleModelsExist() throws Exception {
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
    @DisplayName("ZIP 내부에 FBX와 GLB가 함께 있으면 모델 파일 2개로 보고 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenFbxAndGlbExistTogether() throws Exception {
        // given
        Path zipFile = createZipFile(
            "multiple-model-formats.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("sub/other.glb", "model2".getBytes())
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
        assertThat(result.textures().getFirst().relativePath()).isEqualTo("textures/basecolor.png");
        assertThat(Files.exists(result.model().path())).isTrue();
        assertThat(Files.exists(result.textures().getFirst().path())).isTrue();
    }

    @Test
    @DisplayName("ZIP 내부 폴더 계층이 있는 텍스처는 상대 경로를 보존한다")
    void extractAssetZip_preservesNestedTextureRelativePath() throws Exception {
        // given
        Path zipFile = createZipFile(
            "nested-textures.zip",
            new ZipTestEntry("model/main.fbx", "model".getBytes()),
            new ZipTestEntry("assets/materials/chair/basecolor.png", "texture".getBytes())
        );

        // when
        ZipExtractService.ZipExtractResult result = zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract"));

        // then
        assertThat(result.model().originalName()).isEqualTo("main.fbx");
        assertThat(result.model().relativePath()).isEqualTo("model/main.fbx");
        assertThat(result.textures()).singleElement()
            .satisfies(texture -> {
                assertThat(texture.originalName()).isEqualTo("basecolor.png");
                assertThat(texture.relativePath()).isEqualTo("assets/materials/chair/basecolor.png");
                assertThat(Files.exists(texture.path())).isTrue();
            });
    }

    @Test
    @DisplayName("정상 ZIP이면 GLB도 모델 파일로 분류한다")
    void extractAssetZip_returnsGlbAsModel() throws Exception {
        // given
        Path zipFile = createZipFile(
            "glb-asset.zip",
            new ZipTestEntry("model.glb", "model".getBytes()),
            new ZipTestEntry("textures/basecolor.png", "texture".getBytes())
        );

        // when
        ZipExtractService.ZipExtractResult result = zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract"));

        // then
        assertThat(result.model().fileType()).isEqualTo(AssetFileType.MODEL);
        assertThat(result.model().originalName()).isEqualTo("model.glb");
        assertThat(result.model().extension()).isEqualTo("glb");
        assertThat(result.model().contentType()).isEqualTo("model/gltf-binary");
        assertThat(result.textures()).hasSize(1);
    }

    @Test
    @DisplayName("ZIP 압축 해제 후 총 용량 제한을 초과하면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenTotalExtractedSizeIsTooLarge() throws Exception {
        // given
        ZipExtractService limitedZipExtractService = new ZipExtractService(10L, 100L, 10);
        Path zipFile = createZipFile(
            "too-large-total.zip",
            new ZipTestEntry("model.fbx", "12345".getBytes()),
            new ZipTestEntry("basecolor.png", "123456".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> limitedZipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부 단일 파일 용량 제한을 초과하면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenSingleExtractedFileIsTooLarge() throws Exception {
        // given
        ZipExtractService limitedZipExtractService = new ZipExtractService(100L, 4L, 10);
        Path zipFile = createZipFile(
            "too-large-file.zip",
            new ZipTestEntry("model.fbx", "12345".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> limitedZipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부 파일 개수 제한을 초과하면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenExtractedFileCountIsTooLarge() throws Exception {
        // given
        ZipExtractService limitedZipExtractService = new ZipExtractService(100L, 100L, 1);
        Path zipFile = createZipFile(
            "too-many-files.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("basecolor.png", "texture".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> limitedZipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ZIP 내부 중복 경로 엔트리가 있으면 예외가 발생한다")
    void extractAssetZip_throwsExceptionWhenDuplicateTargetPathExists() throws Exception {
        // given
        Path zipFile = createZipFile(
            "duplicate-entry.zip",
            new ZipTestEntry("model.fbx", "model".getBytes()),
            new ZipTestEntry("textures/basecolor.png", "texture".getBytes()),
            new ZipTestEntry("textures/../textures/basecolor.png", "other".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> zipExtractService.extractAssetZip(zipFile, tempDir.resolve("extract")))
            .isInstanceOf(BusinessException.class);
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
