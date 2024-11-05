package com.kopo.pocket.service;

import com.kopo.pocket.config.LanguageConfig.Language;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@Service
public class BrowserLanguageResolver {
    // public Language resolveLanguage(HttpServletRequest request) {
    //     String acceptLanguage = request.getHeader("Accept-Language");
        
    //     if (acceptLanguage != null) {
    //         // 첫 번째 선호 언어 코드 추출
    //         String primaryLanguage = acceptLanguage.split(",")[0].trim().toLowerCase();
            
    //         // ko, ko-KR 등의 형식 모두 처리
    //         if (primaryLanguage.startsWith("ko")) {
    //             return Language.KR;
    //         }
    //     }
        
    //     return Language.EN; // 기본값은 영어
    // }
    public Language resolveLanguage(HttpServletRequest request) {
        // 기본값으로 항상 한글 반환
        return Language.KR;
    }
}