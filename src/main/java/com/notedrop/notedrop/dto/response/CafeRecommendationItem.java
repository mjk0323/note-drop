package com.notedrop.notedrop.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CafeRecommendationItem {
    private Long   id;
    private String name;
    private String address;
    private String operatingHours;
    private String mapUrl;
    private double maxSimilarityScore;
}
