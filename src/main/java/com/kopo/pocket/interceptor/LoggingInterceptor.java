package com.kopo.pocket.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    // 파일 로깅용 로거
    private static final Logger downloadLogger = LoggerFactory.getLogger("downloadLogger");
    // 콘솔 로깅용 로거
    private static final Logger consoleLogger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 다운로드 로그 API 요청에 대해서만 로깅
        if (request.getRequestURI().equals("/api/log-download")) {
            String clientIP = getClientIP(request);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logMessage = String.format("Deck Download Event - Time: %s, IP: %s", timestamp, clientIP);
            
            // 콘솔에 출력
            //consoleLogger.info(">>> " + logMessage);
            
            // 파일에 기록
            downloadLogger.info(logMessage);
        }
        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP"
        };

        for (String headerName : headerNames) {
            String header = request.getHeader(headerName);
            if (header != null && !header.isEmpty() && !"unknown".equalsIgnoreCase(header)) {
                // 쉼표로 구분된 경우 첫 번째 IP 반환
                return header.split(",")[0].trim();
            }
        }

        // 모든 헤더가 없는 경우 기본 IP 반환
        String remoteAddr = request.getRemoteAddr();
        
        // IPv6 localhost를 IPv4로 변환
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        
        return remoteAddr;
    }
}