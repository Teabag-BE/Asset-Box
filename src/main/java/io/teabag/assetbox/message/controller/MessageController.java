package io.teabag.assetbox.message.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.dto.MessageSendRequest;
import io.teabag.assetbox.message.service.MessageService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
