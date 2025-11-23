package com.yhladkevych.appraise.adapter.telegram;

import com.yhladkevych.appraise.adapter.persistence.repository.JpaUserRepository;
import com.yhladkevych.appraise.domain.model.UserRole;
import com.yhladkevych.appraise.domain.port.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationServiceAdapter implements TelegramNotificationService {
    private final JpaUserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.telegram.bot-token}")
    private String botToken;

    @Value("${app.telegram.api-url}")
    private String apiUrl;

    @Override
    public void sendNotificationToAppraisers(String message, Long orderId) {
        List<Long> appraiserTelegramIds = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.APPRAISER)
                .map(user -> user.getTelegramId())
                .collect(Collectors.toList());

        String fullMessage = String.format("%s\nOrder ID: %d", message, orderId);

        for (Long telegramId : appraiserTelegramIds) {
            try {
                sendMessage(telegramId, fullMessage);
            } catch (Exception e) {
                log.error("Failed to send notification to appraiser with telegram ID: {}", telegramId, e);
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        String url = String.format("%s/bot%s/sendMessage", apiUrl, botToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(url, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Failed to send Telegram message. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending Telegram message to chat ID: {}", chatId, e);
            throw new RuntimeException("Failed to send Telegram notification", e);
        }
    }
}

