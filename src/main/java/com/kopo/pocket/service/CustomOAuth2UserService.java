package com.kopo.pocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.kopo.pocket.dto.UserDTO;
import com.kopo.pocket.enums.AuthProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    // 생성자 주입 방식으로 변경
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("OAuth2 인증 처리 중 오류 발생", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        // Google에서 받아온 정보 추출
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");  // Google의 고유 ID

        log.info("Google OAuth2 User Info - email: {}, name: {}", email, name);

        // 기존 사용자 확인 또는 새 사용자 생성
        UserDTO user = userService.findByProviderAndProviderId(AuthProvider.GOOGLE, googleId);
        
        if (user == null) {
            // 새 사용자 생성
            user = createNewUser(email, name, googleId);
        }

        return oauth2User;
    }

    private UserDTO createNewUser(String email, String name, String googleId) {
        UserDTO newUser = new UserDTO();
        newUser.setEmail(email);
        newUser.setUserId(generateUserId(email));  // 이메일 기반으로 유저ID 생성
        newUser.setProvider(AuthProvider.GOOGLE);
        newUser.setProviderId(googleId);
        newUser.setEmailVerified(true);  // Google 계정은 이미 인증됨
        newUser.setActive(true);
        newUser.setRole("USER");

        return userService.createSocialUser(newUser);
    }

    private String generateUserId(String email) {
        // 이메일의 @ 앞부분을 기본 userId로 사용
        String baseUserId = email.split("@")[0];
        
        // 중복 확인 및 처리
        String userId = baseUserId;
        int suffix = 1;
        
        while (userService.existsByUserId(userId)) {
            userId = baseUserId + suffix++;
        }
        
        return userId;
    }
}