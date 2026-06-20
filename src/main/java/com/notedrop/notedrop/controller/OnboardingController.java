package com.notedrop.notedrop.controller;

import com.notedrop.notedrop.common.response.ApiResponse;
import com.notedrop.notedrop.dto.request.OnboardingRequest;
import com.notedrop.notedrop.dto.response.OnboardingQuestionResponse;
import com.notedrop.notedrop.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/questions")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ApiResponse<OnboardingQuestionResponse> getQuestions() {
        return ApiResponse.success(onboardingService.getQuestions());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> completeOnboarding(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid OnboardingRequest request) {
        onboardingService.completeOnboarding(userId, request);
        return ApiResponse.success(null);
    }
}
