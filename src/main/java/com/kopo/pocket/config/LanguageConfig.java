package com.kopo.pocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class LanguageConfig {
    public enum Language {
        EN("en", "p_pocket_en"),
        KR("kr", "p_pocket_kr");

        private final String code;
        private final String dbName;

        Language(String code, String dbName) {
            this.code = code;
            this.dbName = dbName;
        }

        public String getCode() {
            return code;
        }

        public String getDbName() {
            return dbName;
        }
    }

    @Bean
    @SessionScope
    public LanguageService languageService() {
        return new LanguageService();
    }

    public static class LanguageService {
        private Language currentLanguage = Language.KR;

        public Language getCurrentLanguage() {
            return currentLanguage;
        }

        public void setCurrentLanguage(Language language) {
            this.currentLanguage = language;
        }

        public String getCurrentDbName() {
            return currentLanguage.getDbName();
        }
    }
}