
package com.school.whale.identity.controller;

import com.school.whale.identity.dto.AcceptUserInvitationRequest;
import com.school.whale.identity.dto.AcceptUserInvitationResponse;
import com.school.whale.identity.dto.CreateUserInvitationRequest;
import com.school.whale.identity.dto.UserInvitationResponse;
import com.school.whale.identity.service.UserInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-invitations")
@RequiredArgsConstructor
public class UserInvitationController {

    private final UserInvitationService userInvitationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_INVITATION_CREATE')")
    public UserInvitationResponse createInvitation(
            @Valid @RequestBody CreateUserInvitationRequest request,
            Authentication authentication
    ) {
        return userInvitationService.createInvitation(
                request,
                authentication
        );
    }

    @PostMapping("/accept")
    @PreAuthorize("permitAll()")
    public AcceptUserInvitationResponse acceptInvitation(
            @Valid @RequestBody AcceptUserInvitationRequest request
    ) {
        return userInvitationService.acceptInvitation(request);
    }
}
