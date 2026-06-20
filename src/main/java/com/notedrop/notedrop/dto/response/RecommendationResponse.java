package com.notedrop.notedrop.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecommendationResponse {
    private List<BeanRecommendationItem> beans;
    private List<CafeRecommendationItem> cafes;
}
