package com.kopo.pocket.controller;

import com.kopo.pocket.config.LanguageConfig;
import com.kopo.pocket.service.BrowserLanguageResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class LanguageController {

    @Autowired
    private LanguageConfig.LanguageService languageService;

    @Autowired
    private BrowserLanguageResolver browserLanguageResolver;

    @GetMapping("/current-language")
    public ResponseEntity<LanguageConfig.Language> getCurrentLanguage(
            HttpSession session,
            HttpServletRequest request) {
        LanguageConfig.Language currentLanguage = 
            (LanguageConfig.Language) session.getAttribute("CURRENT_LANGUAGE");
        
        if (currentLanguage == null) {
            // 브라우저 언어 기반으로 초기 언어 설정
            currentLanguage = browserLanguageResolver.resolveLanguage(request);
            session.setAttribute("CURRENT_LANGUAGE", currentLanguage);
            languageService.setCurrentLanguage(currentLanguage);
        }
        
        return ResponseEntity.ok(currentLanguage);
    }

    @PostMapping("/language")
    public ResponseEntity<String> setLanguage(
            @RequestBody LanguageRequest request,
            HttpSession session) {
        try {
            LanguageConfig.Language language = 
                LanguageConfig.Language.valueOf(request.getLanguage());
            languageService.setCurrentLanguage(language);
            session.setAttribute("CURRENT_LANGUAGE", language);
            return ResponseEntity.ok()
                .body("Language updated to: " + language);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Failed to switch language: " + e.getMessage());
        }
    }

    static class LanguageRequest {
        private String language;
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
    }
}