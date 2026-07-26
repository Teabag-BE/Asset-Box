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
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "해당 Refresh Token은 만료되었습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 포함되어있지 않습니다."),
    CAN_NOT_SWITCH_TO_SAME_ROLE(HttpStatus.BAD_REQUEST, "이전과 동일한 Role로 전환이 불가능합니다."),
    FORBIDDEN_SELF_ROLE_CHANGE(HttpStatus.BAD_REQUEST, "본인의 Role을 수정할 수 없습니다."),


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
    NOT_VALID_PROVIDER(HttpStatus.BAD_REQUEST, "올바르지 않은 OAuth 제공자 명입니다."),
    ACCOUNT_NOT_ADMIN(HttpStatus.UNAUTHORIZED, "해당 계정에 어드민 권한이 없습니다."),
    ACCOUNT_NOT_SUPER_ADMIN(HttpStatus.UNAUTHORIZED, "해당 계정에 루트 어드민 권한이 없습니다."),


    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    CATEGORY_DEPTH_INVALID(HttpStatus.BAD_REQUEST, "선택할 수 없는 카테고리입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글을 찾을 수 없습니다."),
    POST_LINKED_REQUEST_INVALID_STATUS (HttpStatus.CONFLICT, "IN_PROGRESS 상태의 요청글만 완료할 수 있습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 요청 게시글을 찾을 수 없습니다."),
    REQUEST_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "REQUESTED 상태의 요청글만 수정할 수 있습니다."),
    REQUEST_NOT_DELETABLE(HttpStatus.CONFLICT, "REQUESTED 상태의 요청글만 삭제할 수 있습니다."),
    REQUEST_NOT_ASSIGNABLE(HttpStatus.CONFLICT, "REQUESTED 상태의 요청글만 수락할 수 있습니다."),
    REQUEST_ASSIGNEE_MISMATCH(HttpStatus.FORBIDDEN, "해당 요청글의 담당자만 처리할 수 있습니다."),
    REQUEST_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료 처리된 요청글입니다."),
    REQUEST_ASSIGN_TAKEN(HttpStatus.CONFLICT, "이미 다른 담당자가 수락한 요청글입니다."),
    REQUEST_ASSIGN_SELF_DUPLICATED(HttpStatus.CONFLICT, "이미 본인이 수락한 요청글입니다."),
    REQUEST_ASSIGN_FORBIDDEN(HttpStatus.FORBIDDEN, "TA 전공 사용자만 요청글을 수락할 수 있습니다."),
    REQUEST_ASSIGN_REQUESTER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 작성한 요청글은 수락할 수 없습니다."),
    REQUEST_COMPLETED_LOCKED(HttpStatus.CONFLICT, "이미 완료된 요청글은 변경할 수 없습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 피드백을 찾을 수 없습니다."),
    TAG_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "태그 이름은 30자를 초과할 수 없습니다."),
    TAG_NAME_INVALID_CHAR(HttpStatus.BAD_REQUEST, "태그 이름에 허용되지 않는 문자가 포함되어 있습니다."),
    LIMIT_TOO_LARGE(HttpStatus.BAD_REQUEST, "조회 가능한 개수를 초과했습니다."),

    // Message
    MESSAGE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신에게 메시지를 보낼 수 없습니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다."),


    // File
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 파일을 찾을 수 없습니다."),
    STORAGE_WRITE_FAILED(HttpStatus.NOT_FOUND, "s3 파일 전송에 실패했습니다."),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "s3 파일 삭제에 실패했습니다."),
    NOT_ENOUGH_FILE(HttpStatus.BAD_REQUEST, "기존의 파일 개수와 정보 수가 맞지 않습니다."),
    NOT_ENOUGH_INFO(HttpStatus.BAD_REQUEST, "파일 개수와 정보 수가 맞지 않습니다."),
    EXTENSIONS_INVALID(HttpStatus.BAD_REQUEST, "허용되지 않은 확장자입니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일입니다."),
    SIZE_INVALID(HttpStatus.BAD_REQUEST, "파일 크기는 50MB를 초과할 수 없습니다."),
    FILE_NAME_EMPTY(HttpStatus.BAD_REQUEST, "파일 이름이 비어있습니다."),
    FILE_TOTAL_SIZE_INVALID(HttpStatus.BAD_REQUEST, "게시글당 업로드 가능한 파일 용량은 50MB를 초과할 수 없습니다"),
    THUMBNAIL_SIZE_INVALID(HttpStatus.BAD_REQUEST, "썸네일 크기는 10MB를 초과할 수 없습니다."),
    ZIP_INVALID(HttpStatus.BAD_REQUEST, "ZIP 파일이 올바르지 않습니다."),
    ZIP_MODEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "ZIP 내부에 모델 파일이 없습니다."),
    ZIP_MODEL_COUNT_INVALID(HttpStatus.BAD_REQUEST, "ZIP 내부에는 모델 파일이 1개만 있어야 합니다."),
    VIEWER_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "미리보기용 MODEL 파일을 찾을 수 없습니다."),
    VIEWER_MODEL_COUNT_INVALID(HttpStatus.CONFLICT, "미리보기용 MODEL 파일은 1개만 존재해야 합니다."),

    NOT_SEQUENTIAL_ORDER(HttpStatus.BAD_REQUEST, "파일 순서 정보가 맞지 않습니다."),


    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    INPUT_NOT_VALID(HttpStatus.BAD_REQUEST, "입력값이 잘못되었습니다.")

    ;

    private final HttpStatus status;
    private final String description;
}
