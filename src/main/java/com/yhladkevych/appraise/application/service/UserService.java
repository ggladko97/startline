package com.yhladkevych.appraise.application.service;

import com.yhladkevych.appraise.domain.exception.UserNotFoundException;
import com.yhladkevych.appraise.domain.model.User;
import com.yhladkevych.appraise.domain.model.UserRole;
import com.yhladkevych.appraise.domain.port.UserRepository;

import java.time.LocalDateTime;
import java.util.Set;

public class UserService {
    private final UserRepository userRepository;
    private final Set<Long> appraiserWhitelist;

    public UserService(UserRepository userRepository, Set<Long> appraiserWhitelist) {
        this.userRepository = userRepository;
        this.appraiserWhitelist = appraiserWhitelist;
    }

    public User createOrLoginUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createUser(telegramId));
    }

    public User createAppraiser(Long telegramId) {
        if (!appraiserWhitelist.contains(telegramId)) {
            throw new IllegalArgumentException("Telegram ID is not in appraiser whitelist");
        }
        return userRepository.findByTelegramId(telegramId)
                .map(user -> {
                    if (user.getRole() != UserRole.APPRAISER) {
                        user.setRole(UserRole.APPRAISER);
                        user.setUpdatedAt(LocalDateTime.now());
                        return userRepository.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .telegramId(telegramId)
                            .role(UserRole.APPRAISER)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public User getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User not found with telegram ID: " + telegramId));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }

    private User createUser(Long telegramId) {
        UserRole role = appraiserWhitelist.contains(telegramId) ? UserRole.APPRAISER : UserRole.CLIENT;
        User user = User.builder()
                .telegramId(telegramId)
                .role(role)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }
}

