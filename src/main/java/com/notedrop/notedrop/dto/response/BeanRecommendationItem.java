package com.notedrop.notedrop.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BeanRecommendationItem {
    private Long   id;
    private String name;
    private String origin;
    private String processingMethod;
    private String roastLevel;
    private double similarityScore;
}