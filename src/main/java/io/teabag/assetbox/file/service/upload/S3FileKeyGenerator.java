package io.teabag.assetbox.file.service.upload;

// 사용자가 올린 원본 파일명을 그대로 쓰지 않고,
// S3에 저장할 안전한 고유 경로를 만들어주는 클래스

import io.teabag.assetbox.file.domain.FilePurpose;

public class S3FileKeyGenerator {

	public String generate(FilePurpose purpose, Long domainId, String originalFilename){

		return " ";
	}

}
