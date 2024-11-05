package com.kopo.pocket.mapper.users;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kopo.pocket.dto.UserDTO;
import com.kopo.pocket.enums.AuthProvider;
import java.time.LocalDateTime;

@Mapper
public interface UserMapper {
    // 기본 CRUD
    void insertUser(UserDTO user);
    UserDTO findByUserId(String userId);
    UserDTO findByEmail(String email);
    
    // 이메일 인증 관련
    UserDTO findByVerificationToken(String token);
    void updateEmailVerified(String userId, boolean verified);
    void clearVerificationToken(String userId);
    LocalDateTime getTokenCreationTime(String token);
    
    // 소셜 로그인 관련
    // UserDTO findByProviderAndProviderId(AuthProvider provider, String providerId);
    
    // 중복 체크
    int countByUserId(String userId);
    int countByEmail(String email, AuthProvider provider);  // provider별 체크
    
    // 상태 관리
    void updateUserActive(String userId, boolean active);
    void updateLastLoginAt(String userId);
    void updatePassword(String userId, String newPassword);

    // 소셜 로그인 관련 메서드 추가
    UserDTO findByProviderAndProviderId(@Param("provider") AuthProvider provider, 
                                      @Param("providerId") String providerId);
    int countByEmailAndProvider(@Param("email") String email, 
                              @Param("provider") AuthProvider provider);

}