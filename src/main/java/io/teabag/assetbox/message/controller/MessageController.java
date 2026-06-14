package io.teabag.assetbox.message.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.message.dto.ConversationSummary;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.dto.MessageSendRequest;
import io.teabag.assetbox.message.dto.UnreadCountResponse;
import io.teabag.assetbox.message.service.MessageService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageResponse> send(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody MessageSendRequest request
    ) {
        return ApiResponse.created(
                messageService.send(currentUser.getId(), request.toUserId(), request.content()),
                SuccessCode.MESSAGE_CREATED.getSuccessMessage()
        );
    }

    @GetMapping("/conversation/{partnerId}")
    public ApiResponse<List<MessageResponse>> conversation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long partnerId
    ) {
        return ApiResponse.ok(
                messageService.getConversation(currentUser.getId(), partnerId),
                SuccessCode.MESSAGE_CONVERSATION_READ.getSuccessMessage()
        );
    }

    @GetMapping("/inbox")
    public ApiResponse<List<ConversationSummary>> inbox(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(
                messageService.getInbox(currentUser.getId()),
                SuccessCode.MESSAGE_INBOX_READ.getSuccessMessage()
        );
    }

    @GetMapping("/unread")
    public ApiResponse<UnreadCountResponse> unread(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(
                messageService.getUnreadCount(currentUser.getId()),
                SuccessCode.MESSAGE_UNREAD_COUNT_READ.getSuccessMessage()
        );
    }

    @PatchMapping("/conversation/{partnerId}/read")
    public ApiResponse<Void> markConversationAsRead(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long partnerId
    ) {
        messageService.markConversationAsRead(currentUser.getId(), partnerId);
        return ApiResponse.ok(null, SuccessCode.MESSAGE_READ.getSuccessMessage());
    }
}
