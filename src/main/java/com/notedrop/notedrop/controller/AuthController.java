package com.notedrop.notedrop.controller;

import com.notedrop.notedrop.common.response.ApiResponse;
import com.notedrop.notedrop.dto.request.LoginRequest;
import com.notedrop.notedrop.dto.request.SignupRequest;
import com.notedrop.notedrop.dto.response.AuthResponse;
import com.notedrop.notedrop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("Authorization") String bearerToken) {
        String refreshToken = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7) : bearerToken;
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(refreshToken)));
    }
}
