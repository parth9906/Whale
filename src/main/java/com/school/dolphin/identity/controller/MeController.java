package com.school.whale.identity.controller;

import com.school.whale.identity.dto.AccessContextResponse;
import com.school.whale.identity.service.AccessContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final AccessContextService accessContextService;

    @GetMapping("/access-context")
    public AccessContextResponse getAccessContext(
            Authentication authentication
    ) {
        return accessContextService.getCurrentAccessContext(authentication);
    }
}