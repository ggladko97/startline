package com.yhladkevych.appraise.adapter.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
public class TelegramPlatformFilter extends OncePerRequestFilter {
    private static final String TELEGRAM_USER_AGENT = "TelegramBot";

    @Value("${app.security.telegram-platform-only:false}")
    private boolean telegramPlatformOnly;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (telegramPlatformOnly) {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || !userAgent.contains(TELEGRAM_USER_AGENT)) {
                log.warn("Request rejected - not from Telegram platform. User-Agent: {}", userAgent);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Access denied. Only Telegram platform is allowed.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}


