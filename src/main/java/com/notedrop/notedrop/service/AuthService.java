package com.notedrop.notedrop.service;

import com.notedrop.notedrop.config.JwtTokenProvider;
import com.notedrop.notedrop.dto.request.LoginRequest;
import com.notedrop.notedrop.dto.request.SignupRequest;
import com.notedrop.notedrop.dto.response.AuthResponse;
import com.notedrop.notedrop.entity.User;
import com.notedrop.notedrop.exception.BusinessException;
import com.notedrop.notedrop.exception.ErrorCode;
import com.notedrop.notedrop.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
        userRepository.save(User.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        ));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResponse(
                jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name()),
                jwtTokenProvider.generateRefreshToken(user.getEmail())
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        Claims claims = jwtTokenProvider.getClaims(refreshToken);
        if (claims.get("role", String.class) != null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new AuthResponse(
                jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name()),
                jwtTokenProvider.generateRefreshToken(user.getEmail())
        );
    }
}
