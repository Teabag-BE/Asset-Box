package io.teabag.assetbox.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuccessCode {

    // USER
    USER_CREATED("계정이 정상적으로 생성되었습니다."),
    USER_SIGNIN("계정에 성공적으로 로그인되었습니다"),
    USER_READ("계정이 성공적으로 조회되었습니다"),


    // POST
    POST_CREATED("게시물이 정상적으로 생성되었습니다."),
    POST_UPDATED("게시물이 정상적으로 수정되었습니다."),
    POST_READ("게시물들이 정상적으로 조회되었습니다."),
    POST_READ_SINGLE("게시물이 정상적으로 조회되었습니다."),

    // REQUEST
    REQUEST_CREATED("요청글이 정상적으로 생성되었습니다."),
    REQUEST_READ("요청글들이 정상적으로 조회되었습니다."),
    REQUEST_READ_SINGLE("요청글이 정상적으로 조회되었습니다."),

    // FILE
    FILE_ISSUE_PRESIGNED_URL("파일을 다운로드하는 Presigned URL이 정상적으로 발급되었습니다."),

    // Category
    CATEGORY_READ("카테고리의 대분류가 정상적으로 조회되었습니다."),
    CATEGORY_READ_ALL("전체 카테고리가 조회되었습니다."),
    CATEGORY_CHILDREN_READ("해당 카테고리의 자식카테고리가 정상적으로 조회되었습니다."),

    // Message
    MESSAGE_CREATED("메시지가 정상적으로 전송되었습니다."),
    MESSAGE_CONVERSATION_READ("대화 내역이 정상적으로 조회되었습니다."),
    MESSAGE_INBOX_READ("대화방 목록이 정상적으로 조회되었습니다."),
    MESSAGE_UNREAD_COUNT_READ("안 읽은 메시지 수가 정상적으로 조회되었습니다."),
    MESSAGE_READ("메시지가 정상적으로 읽음 처리되었습니다.")

    ;

    private final String successMessage;
}
