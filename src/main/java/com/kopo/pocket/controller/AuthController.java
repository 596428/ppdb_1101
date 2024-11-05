package com.kopo.pocket.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// import org.apache.ibatis.mapping.Environment;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import com.kopo.pocket.dto.UserDTO;
import com.kopo.pocket.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.kopo.pocket.service.EmailService;
import com.kopo.pocket.enums.AuthProvider;

import java.io.IOException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

//import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private Environment environment;
    
    @Value("${app.domain}")
    private String appDomain;

    @PostMapping("/signup/email")
    public ResponseEntity<UserDTO.SignupResponse> emailSignup(@RequestBody UserDTO.EmailSignupRequest request) {
        log.info("Received email signup request: {}", request);
        
        // 1. 기본 유효성 검증
        String validationError = validateSignupRequest(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(
                createSignupResponse(false, validationError, null, AuthProvider.LOCAL));
        }
        
        // 2. 아이디/이메일 중복 체크
        if (userService.existsByUserId(request.getUserId())) {
            return ResponseEntity.badRequest().body(
                createSignupResponse(false, "이미 사용 중인 아이디입니다.", null, AuthProvider.LOCAL));
        }
        if (userService.existsByEmail(request.getEmail(), AuthProvider.LOCAL)) {
            return ResponseEntity.badRequest().body(
                createSignupResponse(false, "이미 사용 중인 이메일입니다.", null, AuthProvider.LOCAL));
        }
        
        try {
            // 3. 사용자 생성
            UserDTO user = userService.createLocalUser(request);
            
            // 4. 인증 이메일 발송
            emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUserId(),
                user.getVerificationToken()
            );
            
            return ResponseEntity.ok(createSignupResponse(true, 
                "회원가입이 완료되었습니다. 이메일을 확인해주세요.", 
                user.getUserId(), 
                AuthProvider.LOCAL));
                
        } catch (Exception e) {
            log.error("Error during signup: ", e);
            return ResponseEntity.internalServerError().body(
                createSignupResponse(false, 
                    "회원가입 처리 중 오류가 발생했습니다: " + e.getMessage(), 
                    null, 
                    AuthProvider.LOCAL));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok("이메일 인증이 완료되었습니다. 이제 로그인하실 수 있습니다.");
        } catch (Exception e) {
            log.error("Error during email verification: ", e);
            return ResponseEntity.badRequest().body(
                e.getMessage().contains("expired") ? 
                "인증 링크가 만료되었습니다." : "유효하지 않은 인증 링크입니다.");
        }
    }

    // 추후 소셜 로그인용 엔드포인트 추가 예정
    // @PostMapping("/signup/social")
    // public ResponseEntity<UserDTO.SignupResponse> socialSignup(...) {
    //     // 소셜 로그인 처리
    // }

    private String validateSignupRequest(UserDTO.EmailSignupRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return "아이디를 입력해주세요.";
        }
        if (request.getUserId().length() < 4) {
            return "아이디는 4자 이상이어야 합니다.";
        }
        if (!request.getUserId().matches("^[a-zA-Z0-9_-]{4,20}$")) {
            return "아이디는 영문, 숫자, 특수문자(-_)만 사용 가능합니다.";
        }
        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return "유효한 이메일 주소를 입력해주세요.";
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            return "비밀번호는 8자 이상이어야 합니다.";
        }
        if (!request.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$")) {
            return "비밀번호는 영문과 숫자를 포함해야 합니다.";
        }
        return null;
    }

    private UserDTO.SignupResponse createSignupResponse(
            boolean success, 
            String message, 
            String userId, 
            AuthProvider provider) {
        UserDTO.SignupResponse response = new UserDTO.SignupResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setUserId(userId);
        response.setProvider(provider);
        return response;
    }


    //////// 로그인
    //////////
     @PostMapping("/login")
    public ResponseEntity<UserDTO.LoginResponse> login(
            @RequestBody UserDTO.LoginRequest request,
            HttpSession session) {
        
        log.info("Login attempt for user: {}", request.getUserId());
        
        try {
            // 사용자 찾기
            UserDTO user = userService.findByUserId(request.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(
                    createLoginResponse(false, "아이디 또는 비밀번호가 일치하지 않습니다.", null, null));
            }

            // 비밀번호 확인
            if (!userService.verifyPassword(request.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(
                    createLoginResponse(false, "아이디 또는 비밀번호가 일치하지 않습니다.", null, null));
            }

            // 이메일 인증 확인
            if (!user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(
                    createLoginResponse(false, "이메일 인증이 필요합니다.", null, null));
            }

            // 계정 활성화 상태 확인
            if (!user.isActive()) {
                return ResponseEntity.badRequest().body(
                    createLoginResponse(false, "비활성화된 계정입니다.", null, null));
            }

            // 세션 생성
            session.setAttribute("USER_ID", user.getUserId());
            session.setAttribute("USER_ROLE", user.getRole());
            
            // 마지막 로그인 시간 업데이트
            userService.updateLastLoginAt(user.getUserId());

            return ResponseEntity.ok(createLoginResponse(true, 
                "로그인 성공", user.getUserId(), user.getRole()));

        } catch (Exception e) {
            log.error("Login error: ", e);
            return ResponseEntity.internalServerError().body(
                createLoginResponse(false, "로그인 처리 중 오류가 발생했습니다.", null, null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session, HttpServletResponse response) {
        try {
            // USER 객체나 LOGGED_IN 상태 확인
            UserDTO user = (UserDTO) session.getAttribute("USER");
            Boolean isLoggedIn = (Boolean) session.getAttribute("LOGGED_IN");
            
            if (user != null || Boolean.TRUE.equals(isLoggedIn)) {
                // 세션 무효화
                session.invalidate();
                
                // 쿠키 삭제 (필요한 경우)
                Cookie cookie = new Cookie("JSESSIONID", "");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
                
                log.info("User successfully logged out");
                return ResponseEntity.ok()
                    .body(Map.of(
                        "success", true,
                        "message", "로그아웃 되었습니다."
                    ));
            }
            
            log.warn("Logout attempted while not logged in");
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "로그인 상태가 아닙니다."
                ));
                
        } catch (Exception e) {
            log.error("Logout error: ", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "로그아웃 처리 중 오류가 발생했습니다."
                ));
        }
    }

    // 로그인 상태 확인 엔드포인트 추가
    // @GetMapping("/check")
    // public ResponseEntity<Map<String, Object>> checkLoginStatus(HttpSession session) {
    //     try {
    //         UserDTO user = (UserDTO) session.getAttribute("USER");
    //         Boolean isLoggedIn = (Boolean) session.getAttribute("LOGGED_IN");
            
    //         if (user != null && Boolean.TRUE.equals(isLoggedIn)) {
    //             return ResponseEntity.ok(Map.of(
    //                 "loggedIn", true,
    //                 "userId", user.getUserId(),
    //                 "email", user.getEmail(),
    //                 "provider", user.getProvider().toString()
    //             ));
    //         }
            
    //         return ResponseEntity.ok(Map.of("loggedIn", false));
            
    //     } catch (Exception e) {
    //         log.error("Error checking login status: ", e);
    //         return ResponseEntity.internalServerError()
    //             .body(Map.of("loggedIn", false));
    //     }
    // }
    // @GetMapping("/check")
    // public ResponseEntity<Map<String, Object>> checkLoginStatus(HttpSession session) {
    //     UserDTO user = (UserDTO) session.getAttribute("USER");
    //     Boolean isLoggedIn = (Boolean) session.getAttribute("LOGGED_IN");
        
    //     if (user != null && Boolean.TRUE.equals(isLoggedIn)) {
    //         return ResponseEntity.ok(Map.of(
    //             "loggedIn", true,
    //             "userId", user.getUserId(),
    //             "email", user.getEmail()
    //         ));
    //     }
        
    //     return ResponseEntity.ok(Map.of("loggedIn", false));
    // }
    // @GetMapping("/check")
    // public ResponseEntity<?> checkLoginStatus(HttpSession session) {
    //     String userId = (String) session.getAttribute("USER_ID");
    //     if (userId != null) {
    //         UserDTO user = userService.findByUserId(userId);
    //         return ResponseEntity.ok(Map.of(
    //             "loggedIn", true,
    //             "userId", userId,
    //             "role", user.getRole()
    //         ));
    //     }
    //     return ResponseEntity.ok(Map.of("loggedIn", false));
    // }

    private UserDTO.LoginResponse createLoginResponse(
            boolean success, String message, String userId, String role) {
        UserDTO.LoginResponse response = new UserDTO.LoginResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setUserId(userId);
        response.setRole(role);
        return response;
    }

    // @GetMapping("/google")
    // public void googleLogin(HttpServletResponse response) throws IOException {
    //     String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth";
    //     String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
    //     String redirectUri = appDomain + "/api/auth/google/callback";
        
    //     String url = UriComponentsBuilder.fromHttpUrl(googleAuthUrl)
    //         .queryParam("client_id", clientId)
    //         .queryParam("redirect_uri", redirectUri)
    //         .queryParam("response_type", "code")
    //         .queryParam("scope", "email profile")
    //         .toUriString();
        
    //     response.sendRedirect(url);
    // }

    // @GetMapping("/google/callback")
    // public ResponseEntity<?> googleCallback(@RequestParam String code) {
    //     // OAuth2 인증 처리 및 사용자 정보 저장
    //     // 로그인 세션 생성
    //     return ResponseEntity.ok().build();
    // }

    // @GetMapping("/success")
    // public ResponseEntity<?> oauth2LoginSuccess(
    //         @AuthenticationPrincipal OAuth2User oauth2User,
    //         HttpSession session) {
        
    //     String email = oauth2User.getAttribute("email");
    //     UserDTO user = userService.findByEmail(email);
        
    //     if (user != null) {
    //         // 세션 생성
    //         session.setAttribute("USER_ID", user.getUserId());
    //         session.setAttribute("USER_ROLE", user.getRole());
            
    //         return ResponseEntity.ok(Map.of(
    //             "success", true,
    //             "userId", user.getUserId(),
    //             "role", user.getRole()
    //         ));
    //     }
        
    //     return ResponseEntity.badRequest().body(Map.of(
    //         "success", false,
    //         "message", "User not found"
    //     ));
    // }

    // 1. 구글 로그인 페이지로 리다이렉트
    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
        String redirectUri = appDomain + "/api/auth/google/callback";
        
        log.info("Starting Google OAuth2 login process");
        
        String url = UriComponentsBuilder.fromHttpUrl(googleAuthUrl)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "email profile")
            .build()
            .encode()
            .toUriString();
        
        response.sendRedirect(url);
    }

    // 2. 구글 인증 후 콜백 처리
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam String code, 
                             HttpServletResponse response,
                             HttpSession session) throws IOException {
        try {
            String accessToken = getGoogleAccessToken(code);
            Map<String, Object> userInfo = getGoogleUserInfo(accessToken);
            log.info("Received user info from Google: {}", userInfo);
            
            String email = (String) userInfo.get("email");
            String providerId = (String) userInfo.get("sub");
            
            // 먼저 provider와 providerId로 사용자 찾기
            UserDTO user = userService.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId);
            
            if (user == null) {
                // providerId로 찾지 못한 경우에만 새 사용자 생성
                UserDTO.SocialSignupRequest request = new UserDTO.SocialSignupRequest();
                request.setEmail(email);
                request.setProvider(AuthProvider.GOOGLE);
                request.setProviderId(providerId);
                
                try {
                    user = userService.createSocialUser(request);
                } catch (Exception e) {
                    // 이미 존재하는 이메일인 경우
                    user = userService.findByEmail(email);
                    if (user != null && !AuthProvider.GOOGLE.equals(user.getProvider())) {
                        log.warn("Email already exists with different provider: {}", user.getProvider());
                        response.sendRedirect("/login?error=" + 
                            URLEncoder.encode("이미 다른 방식으로 가입된 이메일입니다.", StandardCharsets.UTF_8));
                        return;
                    }
                }
            }

            if (user != null) {
                // 세션에 사용자 정보 저장
                session.setAttribute("USER", user);
                session.setAttribute("LOGGED_IN", true);
                
                // 로그인 시간 업데이트
                userService.updateLastLoginAt(user.getUserId());
                
                log.info("Successfully logged in user: {}", user.getUserId());
                response.sendRedirect("/");  // 메인 페이지로 리다이렉트
            } else {
                log.error("Failed to create or find user");
                response.sendRedirect("/login?error=" + 
                    URLEncoder.encode("로그인 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8));
            }
            
        } catch (Exception e) {
            log.error("Error during Google callback processing", e);
            response.sendRedirect("/login?error=" + 
                URLEncoder.encode("소셜 로그인 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(HttpSession session) {
        try {
            UserDTO user = (UserDTO) session.getAttribute("USER");
            Boolean isLoggedIn = (Boolean) session.getAttribute("LOGGED_IN");
            
            log.debug("Checking login status - User: {}, LoggedIn: {}", 
                     user != null ? user.getUserId() : "null", 
                     isLoggedIn);
            
            if (user != null && Boolean.TRUE.equals(isLoggedIn)) {
                Map<String, Object> response = new HashMap<>();
                response.put("loggedIn", true);
                response.put("userId", user.getUserId());
                response.put("email", user.getEmail());
                response.put("provider", user.getProvider().toString());
                
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(Map.of("loggedIn", false));
            
        } catch (Exception e) {
            log.error("Error checking login status: ", e);
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
    }

    // 3. 로그인 성공 처리
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("USER");
        if (user != null) {
            Map<String, Object> response = Map.of(
                "success", true,
                "userId", user.getUserId(),
                "email", user.getEmail()
            );
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(Map.of("success", false));
    }

    // 구글 액세스 토큰 받기
    private String getGoogleAccessToken(String code) {
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
        String clientSecret = environment.getProperty("spring.security.oauth2.client.registration.google.client-secret");
        String redirectUri = appDomain + "/api/auth/google/callback";
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String url = "https://oauth2.googleapis.com/token";
        
        Map<String, Object> params = Map.of(
            "code", code,
            "client_id", clientId,
            "client_secret", clientSecret,
            "redirect_uri", redirectUri,
            "grant_type", "authorization_code"
        );
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            url, 
            new HttpEntity<>(params, headers), 
            String.class
        );
        
        JSONObject jsonResponse = new JSONObject(response.getBody());
        return jsonResponse.getString("access_token");
    }

    // 구글 사용자 정보 가져오기
    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        
        return response.getBody();
    }

}