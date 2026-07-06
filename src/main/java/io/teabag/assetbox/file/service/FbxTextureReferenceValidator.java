package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 업로드된 FBX가 참조하는 텍스처가 ZIP에 실제로 들어있는지 1차 검증한다.
 *
 * <p>깨진 업로드(아티스트가 자기 PC 라이브러리 텍스처에 링크한 채 export → ZIP엔 다른 이름의
 * 텍스처만 담아 올림)를 업로드 시점에 차단하기 위한 것. 이 경우 뷰어는 텍스처 없이 렌더된다.
 *
 * <p>판정:
 * <ul>
 *   <li>FBX가 아니면(GLB 등 자체 포함 포맷) 검증 스킵</li>
 *   <li>FBX에 내장(embedded) 이미지가 있으면 자체 완결 → 스킵</li>
 *   <li>외부 참조 텍스처가 ZIP과 <b>하나도</b> 안 맞으면 차단</li>
 *   <li>일부만 누락이면 통과 + 경고 로그(렌더는 되므로 막지 않음)</li>
 * </ul>
 */
@Slf4j
final class FbxTextureReferenceValidator {

    // FBX 안에 저장된 텍스처 경로 문자열(절대/상대 모두)에서 파일명을 뽑기 위한 패턴.
    private static final Pattern IMAGE_REFERENCE = Pattern.compile(
        "[^\\x00-\\x1f\"]*\\.(?:png|jpe?g|tga|tif|tiff|bmp|dds)",
        Pattern.CASE_INSENSITIVE);

    // 내장 이미지 매직 바이트: PNG(89 50 4E 47), JPEG(FF D8 FF)
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private FbxTextureReferenceValidator() {
    }

    /**
     * @param modelPath            추출된 모델 파일 경로
     * @param modelExtension       모델 확장자(소문자)
     * @param textureOriginalNames ZIP에서 추출된 텍스처 파일명(또는 상대경로) 목록
     * @throws BusinessException FBX 참조 텍스처가 ZIP에 하나도 없을 때
     */
    static void validate(Path modelPath, String modelExtension, List<String> textureOriginalNames) {
        if (modelExtension == null || !modelExtension.equalsIgnoreCase("fbx")) {
            return; // GLB 등은 자체 포함 → 검증 불필요
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(modelPath);
        } catch (IOException e) {
            // 검증용 읽기 실패는 업로드 자체를 막지 않는다(안전 우선).
            log.warn("FBX 텍스처 검증용 읽기 실패, 검증을 건너뜁니다: {}", modelPath, e);
            return;
        }

        if (hasEmbeddedImage(bytes)) {
            return; // 텍스처를 FBX에 내장 → 외부 텍스처 불필요
        }

        Set<String> referencedNames = extractReferencedBasenames(bytes);
        if (referencedNames.isEmpty()) {
            return; // 참조하는 외부 텍스처가 없음
        }

        Set<String> presentNames = new HashSet<>();
        for (String name : textureOriginalNames) {
            addWithJpgJpegAlias(presentNames, basename(name).toLowerCase(Locale.ROOT));
        }

        List<String> missing = referencedNames.stream()
            .filter(ref -> !presentNames.contains(ref))
            .toList();

        if (missing.size() == referencedNames.size()) {
            // FBX가 참조하는 텍스처가 ZIP에 하나도 없음 = 깨진 업로드
            throw new BusinessException(ErrorCode.ZIP_TEXTURE_REFERENCE_MISSING);
        }

        if (!missing.isEmpty()) {
            // 일부 누락은 렌더에 치명적이지 않으므로 통과시키되 경고로 남긴다.
            log.warn("FBX가 참조하는 텍스처 일부가 ZIP에 없습니다(통과). model={}, missing={}",
                basename(modelPath.toString()), missing);
        }
    }

    private static boolean hasEmbeddedImage(byte[] bytes) {
        return indexOf(bytes, PNG_MAGIC) >= 0 || indexOf(bytes, JPEG_MAGIC) >= 0;
    }

    private static Set<String> extractReferencedBasenames(byte[] bytes) {
        // FBX는 바이너리여도 텍스처 경로가 읽을 수 있는 문자열로 저장되므로 ISO-8859-1로 훑는다.
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        Matcher matcher = IMAGE_REFERENCE.matcher(text);
        Set<String> names = new HashSet<>();
        while (matcher.find()) {
            String name = basename(matcher.group()).toLowerCase(Locale.ROOT).trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private static void addWithJpgJpegAlias(Set<String> set, String name) {
        set.add(name);
        if (name.endsWith(".jpg")) {
            set.add(name.substring(0, name.length() - 4) + ".jpeg");
        } else if (name.endsWith(".jpeg")) {
            set.add(name.substring(0, name.length() - 5) + ".jpg");
        }
    }

    private static String basename(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    // 바이트 배열에서 패턴의 첫 위치를 찾는다(없으면 -1).
    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
