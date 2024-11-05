package com.kopo.pocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 위치 설정
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
                
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
                
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}