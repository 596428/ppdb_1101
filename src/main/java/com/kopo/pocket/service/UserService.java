package com.kopo.pocket.service;

import com.kopo.pocket.dto.UserDTO;
import com.kopo.pocket.enums.AuthProvider;
import com.kopo.pocket.mapper.users.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    
    // @Autowired
    // private UserMapper userMapper;
    
    // @Autowired
    // private PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 생성자 주입 방식으로 변경
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // 이메일 회원가입
    public UserDTO createLocalUser(UserDTO.EmailSignupRequest request) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(request.getUserId());
        userDTO.setEmail(request.getEmail());
        userDTO.setPassword(passwordEncoder.encode(request.getPassword()));
        userDTO.setProvider(AuthProvider.LOCAL);
        userDTO.setVerificationToken(generateVerificationToken());
        userDTO.setEmailVerified(false);
        userDTO.setActive(false);
        userDTO.setRole("USER");
        
        userMapper.insertUser(userDTO);
        return userDTO;
    }


    // 이메일 중복 체크 (provider 구분)
    public boolean existsByEmail(String email, AuthProvider provider) {
        return userMapper.countByEmail(email, provider) > 0;
    }

    // 아이디 중복 체크
    public boolean existsByUserId(String userId) {
        return userMapper.countByUserId(userId) > 0;
    }

    // 인증 토큰 생성
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    // // 사용자 ID 자동 생성 (소셜 로그인용)
    // private String generateUserId(String email) {
    //     return email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 8);
    // }

    // 이메일 인증 처리
    public void verifyEmail(String token) {
        UserDTO user = userMapper.findByVerificationToken(token);
        if (user == null || !AuthProvider.LOCAL.equals(user.getProvider())) {
            throw new RuntimeException("Invalid verification token");
        }

        // 토큰 만료 체크 (24시간)
        LocalDateTime tokenCreationTime = userMapper.getTokenCreationTime(token);
        if (tokenCreationTime.plusHours(24).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        userMapper.updateEmailVerified(user.getUserId(), true);
        userMapper.updateUserActive(user.getUserId(), true);
        userMapper.clearVerificationToken(user.getUserId());
    }

    // 사용자 생성
    public void createUser(UserDTO userDTO) {
        if (AuthProvider.LOCAL.equals(userDTO.getProvider())) {
            // 로컬 회원가입인 경우 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
            userDTO.setPassword(encodedPassword);
        }
        userMapper.insertUser(userDTO);
    }

    // 로그인 시 비밀번호 검증
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // 비밀번호 변경
    public void changePassword(String userId, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encodedPassword);
    }

    // 사용자 찾기
    public UserDTO findByUserId(String userId) {
        return userMapper.findByUserId(userId);
    }

    // 사용자 상태 업데이트
    // public void updateLastLoginAt(String userId) {
    //     userMapper.updateLastLoginAt(userId);
    // }

    // 소셜 로그인 관련 메서드 추가
    public UserDTO findByProviderAndProviderId(AuthProvider provider, String providerId) {
        try {
            return userMapper.findByProviderAndProviderId(provider, providerId);
        } catch (Exception e) {
            log.error("Error finding user by provider: {} and providerId: {}", provider, providerId, e);
            return null;
        }
    }

    // public UserDTO findByEmail(String email) {
    //     return userMapper.findByEmail(email);
    // }

    // 소셜 회원가입
    public UserDTO createSocialUser(UserDTO.SocialSignupRequest request) {
        try {
            // 이미 존재하는 사용자인지 확인
            UserDTO existingUser = userMapper.findByProviderAndProviderId(
                request.getProvider(), 
                request.getProviderId()
            );
            
            if (existingUser != null) {
                log.info("User already exists with provider: {} and providerId: {}", 
                    request.getProvider(), request.getProviderId());
                return existingUser;
            }

            // 새 사용자 생성
            UserDTO newUser = new UserDTO();
            newUser.setEmail(request.getEmail());
            newUser.setProvider(request.getProvider());
            newUser.setProviderId(request.getProviderId());
            newUser.setEmailVerified(true);
            newUser.setActive(true);
            newUser.setRole("USER");
            
            // userId 생성 (이메일의 @ 앞부분 사용)
            String baseUserId = request.getEmail().split("@")[0];
            String userId = generateUniqueUserId(baseUserId);
            newUser.setUserId(userId);

            // DB에 저장
            userMapper.insertUser(newUser);
            log.info("Created new social user with userId: {}", userId);

            return newUser;

        } catch (Exception e) {
            log.error("Error creating social user: {}", e.getMessage(), e);
            throw new RuntimeException("소셜 계정 생성 중 오류가 발생했습니다.", e);
        }
    }

     // UserDTO를 직접 받는 메서드 (내부 서비스에서 사용)
     public UserDTO createSocialUser(UserDTO userDTO) {
        // 중복 체크
        if (existsByEmailAndProvider(userDTO.getEmail(), userDTO.getProvider())) {
            throw new RuntimeException("이미 존재하는 계정입니다.");
        }

        if (userDTO.getUserId() == null) {
            userDTO.setUserId(generateUniqueUserId(userDTO.getEmail()));
        }

        userMapper.insertUser(userDTO);
        return userDTO;
    }

    // 이메일로 고유한 userId 생성
    private String generateUniqueUserId(String email) {
        String baseUserId = email.split("@")[0];
        String userId = baseUserId;
        int suffix = 1;

        while (existsByUserId(userId)) {
            userId = baseUserId + suffix++;
        }

        return userId;
    }
    // private String generateUniqueUserId(String baseUserId) {
    //     String userId = baseUserId;
    //     int suffix = 1;
        
    //     while (userMapper.countByUserId(userId) > 0) {
    //         userId = baseUserId + suffix++;
    //     }
        
    //     return userId;
    // }

    // Provider별 이메일 중복 체크
    public boolean existsByEmailAndProvider(String email, AuthProvider provider) {
        return userMapper.countByEmailAndProvider(email, provider) > 0;
    }

    // 로그인 처리 - 소셜 로그인 지원
    public UserDTO login(UserDTO.LoginRequest request) {
        UserDTO user;
        if (request.getProvider() != null) {
            // 소셜 로그인
            user = findByProviderAndProviderId(request.getProvider(), request.getProviderId());
        } else {
            // 일반 로그인
            user = findByUserId(request.getUserId());
            if (user == null || !verifyPassword(request.getPassword(), user.getPassword())) {
                return null;
            }
        }

        if (user != null && user.isActive()) {
            updateLastLoginAt(user.getUserId());
        }

        return user;
    }

    public void updateLastLoginAt(String userId) {
        try {
            userMapper.updateLastLoginAt(userId);
        } catch (Exception e) {
            log.error("Error updating last login time for user: {}", userId, e);
        }
    }

    public UserDTO findByEmail(String email) {
        try {
            return userMapper.findByEmail(email);
        } catch (Exception e) {
            log.error("Error finding user by email: {}", email, e);
            return null;
        }
    }
}