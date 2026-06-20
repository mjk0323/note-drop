package com.notedrop.notedrop.controller;

import com.notedrop.notedrop.common.response.ApiResponse;
import com.notedrop.notedrop.dto.response.RecommendationResponse;
import com.notedrop.notedrop.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Cacheable(cacheNames = "recommendations", key = "#userId")
    public ApiResponse<RecommendationResponse> getRecommendations(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(recommendationService.getRecommendations(userId));
    }
}
