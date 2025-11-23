package com.yhladkevych.appraise.config;

import com.yhladkevych.appraise.adapter.web.filter.TelegramPlatformFilter;
import com.yhladkevych.appraise.adapter.web.filter.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter traceIdFilter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(traceIdFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TelegramPlatformFilter> telegramPlatformFilterRegistration(
            TelegramPlatformFilter telegramPlatformFilter) {
        FilterRegistrationBean<TelegramPlatformFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(telegramPlatformFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}


