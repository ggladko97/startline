package com.yhladkevych.appraise.config;

import com.yhladkevych.appraise.application.service.OrderService;
import com.yhladkevych.appraise.application.service.ReportService;
import com.yhladkevych.appraise.application.service.UserService;
import com.yhladkevych.appraise.domain.port.EventPublisher;
import com.yhladkevych.appraise.domain.port.OrderRepository;
import com.yhladkevych.appraise.domain.port.ReportRepository;
import com.yhladkevych.appraise.domain.port.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    public UserService userService(
            UserRepository userRepository,
            @Value("${app.telegram.appraiser-whitelist}") String appraiserWhitelist) {
        Set<Long> whitelist = Arrays.stream(appraiserWhitelist.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        return new UserService(userRepository, whitelist);
    }

    @Bean
    public OrderService orderService(OrderRepository orderRepository, EventPublisher eventPublisher) {
        return new OrderService(orderRepository, eventPublisher);
    }

    @Bean
    public ReportService reportService(ReportRepository reportRepository, OrderRepository orderRepository) {
        return new ReportService(reportRepository, orderRepository);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

