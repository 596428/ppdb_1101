package com.kopo.pocket.config;

import com.kopo.pocket.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
               .addPathPatterns("/api/log-download")  // 다운로드 로그 API만 인터셉트
               .excludePathPatterns("/css/**", "/js/**", "/images/**", "/dataset/**");  // 정적 리소스 제외
    }
}
