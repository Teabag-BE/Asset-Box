package io.teabag.assetbox.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // User 쪽
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    USER_EMAIL_NOT_WHITELISTED(HttpStatus.FORBIDDEN, "가입이 허용된 이메일주소가 아닙니다."),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "잘못된 비밀번호 또는 존재하지 않는 이메일입니다."),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 계정입니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "현재 계정에 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 계정을 찾을 수 없습니다."),
    NOT_SAME_ORIGIN_PROVIDER(HttpStatus.BAD_REQUEST, "계정의 이메일 도메인과 다른 도메인에서 로그인을 시도했습니다."),
    NOT_REGISTERED(HttpStatus.NOT_FOUND, "가입되어 있지 않은 계정입니다."),
    AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "인증 과정 중 오류가 발생했습니다."),
    NOT_SUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원되지 않는 OAuth 제공자입니다. ( NAVER, GOOGLE )"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "해당 토큰은 만료된 토큰입니다."),
    ERROR_FROM_TOKEN(HttpStatus.UNAUTHORIZED, "토큰에서 문제가 발생했습니다."),
    ABNORMAL_TOKEN(HttpStatus.UNAUTHORIZED, "형식이 올바르지 않은 토큰입니다."),


    //Post 쪽
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글을 찾을 수 없습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 요청 게시글을 찾을 수 없습니다."),
    REQUEST_NOT_DELETABLE(HttpStatus.CONFLICT, "REQUESTED 상태의 요청글만 삭제할 수 있습니다."),

    //Category 쪽
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    CATEGORY_DEPTH_INVALID(HttpStatus.BAD_REQUEST, "선택할 수 없는 카테고리입니다."),

    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 피드백을 찾을 수 없습니다."),

    // File
    STORAGE_WRITE_FAILED(HttpStatus.NOT_FOUND, "s3 파일 전송에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 파일을 찾을 수 없습니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    ;

    private final HttpStatus status;
    private final String description;
}
