package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.dto.AdminsUserDetailResponse;
import io.teabag.assetbox.user.dto.Paging;
import io.teabag.assetbox.user.dto.UserUpdateRoleRequest;
import io.teabag.assetbox.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.MemberSubstitution;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<AdminsUserDetailResponse>> getUserDetails(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid Paging paging,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String q
    ) {
        log.info("체크용1");
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.getUserDetailsByAdmin(
                                currentUser.getEmail(),
                                paging.toPageable(),
                                q,
                                role
                        ),
                        SuccessCode.USER_DETAIL_READED.getSuccessMessage()
                )
        );
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Void>> switchToAdmin(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRoleRequest request
    ){
        userService.switchRole(id,currentUser.getEmail(),request.role());
        return ResponseEntity.ok(
                ApiResponse.ok(SuccessCode.USER_ROLE_UPDATED.getSuccessMessage())
        );
    }
}
