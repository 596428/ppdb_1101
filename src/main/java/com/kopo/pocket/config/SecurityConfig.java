package com.kopo.pocket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.kopo.pocket.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    // 생성자 주입 방식으로 변경
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/cardlist",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/dataset/**",
                    "/static/**",
                    "/api/auth/**",
                    "/api/language",
                    "/api/current-language",
                    "/api/cards",
                    "/api/card/**",
                    "/api/filter-options",
                    "/api/log-download",
                    "/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
            // .headers(headers -> headers
            //     .contentSecurityPolicy(csp -> csp
            //         .policyDirectives("default-src 'self'; " +
            //             "script-src 'self' https://accounts.google.com; " +
            //             "frame-src 'self' https://accounts.google.com; " +
            //             "connect-src 'self' https://accounts.google.com; " +
            //             "img-src 'self' https: data:; " +
            //             "style-src 'self' 'unsafe-inline' https:;"))
            // );

        return http.build();
    }
}