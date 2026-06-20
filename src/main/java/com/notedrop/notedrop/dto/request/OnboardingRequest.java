package com.notedrop.notedrop.dto.request;

import com.notedrop.notedrop.common.enums.UserLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class OnboardingRequest {

    @NotNull
    private UserLevel level;

    @NotNull
    private Map<String, List<String>> answers;

    private String preferredBrand;
}