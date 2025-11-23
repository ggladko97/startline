package com.yhladkevych.appraise.domain.port;

public interface TelegramNotificationService {
    void sendNotificationToAppraisers(String message, Long orderId);
}


