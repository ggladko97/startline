package com.yhladkevych.appraise.domain.port;

import com.yhladkevych.appraise.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findById(Long id);
}


