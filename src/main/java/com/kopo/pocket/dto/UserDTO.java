package com.kopo.pocket.dto;

import com.kopo.pocket.enums.AuthProvider;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String userId;
    private String password;           // 소셜 로그인은 null
    private String email;
    private String verificationToken;  // 이메일 인증시에만 사용
    private boolean emailVerified;
    private AuthProvider provider;     // 인증 제공자
    private String providerId;         // 소셜 로그인 ID
    private boolean isActive;
    private String role;
    private String friendCode;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // 이메일 회원가입용 Request
    @Data
    public static class EmailSignupRequest {
        private String userId;
        private String email;
        private String password;
    }

    // 소셜 로그인용 Request
    @Data
    public static class SocialSignupRequest {
        private AuthProvider provider;
        private String providerId;
        private String email;
        private String userId;         // 선택적: 자동 생성 가능
    }

    // 회원가입 응답
    @Data
    public static class SignupResponse {
        private boolean success;
        private String message;
        private String userId;
        private AuthProvider provider;
    }

    // 로그인 요청
    @Data
    public static class LoginRequest {
        private String userId;         // 이메일 로그인용
        private String password;       // 이메일 로그인용
        private AuthProvider provider; // 소셜 로그인용
        private String providerId;     // 소셜 로그인용
    }

    // 로그인 응답 DTO
    @Data
    public static class LoginResponse {
        private boolean success;
        private String message;
        private String userId;
        private String role;
        private AuthProvider provider;
    }
}