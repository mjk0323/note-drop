package com.notedrop.notedrop.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingQuestionResponse {

    private List<Question> beginner;
    private List<Question> enthusiast;
    private List<String> brands;

    @Getter
    @Builder
    public static class Question {
        private String id;
        private String text;
        private boolean multiSelect;
        private List<Option> options;
    }

    @Getter
    @Builder
    public static class Option {
        private String key;
        private String label;
    }
}