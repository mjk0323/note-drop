package com.notedrop.notedrop.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    TASTE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "취향 프로필이 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ONBOARDING_ALREADY_COMPLETE(HttpStatus.CONFLICT, "이미 온보딩을 완료하였습니다."),
    STOCK_EXHAUSTED(HttpStatus.CONFLICT, "재고가 소진되었습니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;
}