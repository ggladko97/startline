package com.yhladkevych.appraise.application.service;

import com.yhladkevych.appraise.domain.exception.UserNotFoundException;
import com.yhladkevych.appraise.domain.model.User;
import com.yhladkevych.appraise.domain.model.UserRole;
import com.yhladkevych.appraise.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private Set<Long> appraiserWhitelist;

    @BeforeEach
    void setUp() {
        appraiserWhitelist = Set.of(123456789L, 987654321L);
        userService = new UserService(userRepository, appraiserWhitelist);
    }

    @Test
    void createOrLoginUser_WhenUserExists_ShouldReturnExistingUser() {
        Long telegramId = 111222333L;
        User existingUser = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .role(UserRole.CLIENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(existingUser));

        User result = userService.createOrLoginUser(telegramId);

        assertEquals(existingUser, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createOrLoginUser_WhenUserNotExists_ShouldCreateNewClient() {
        Long telegramId = 111222333L;
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = userService.createOrLoginUser(telegramId);

        assertEquals(UserRole.CLIENT, result.getRole());
        assertEquals(telegramId, result.getTelegramId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createOrLoginUser_WhenUserNotExistsAndInWhitelist_ShouldCreateAppraiser() {
        Long telegramId = 123456789L;
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = userService.createOrLoginUser(telegramId);

        assertEquals(UserRole.APPRAISER, result.getRole());
        assertEquals(telegramId, result.getTelegramId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAppraiser_WhenNotInWhitelist_ShouldThrowException() {
        Long telegramId = 111222333L;

        assertThrows(IllegalArgumentException.class, () -> userService.createAppraiser(telegramId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAppraiser_WhenInWhitelistAndNotExists_ShouldCreateAppraiser() {
        Long telegramId = 123456789L;
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = userService.createAppraiser(telegramId);

        assertEquals(UserRole.APPRAISER, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAppraiser_WhenExistsAsClient_ShouldUpdateToAppraiser() {
        Long telegramId = 123456789L;
        User existingUser = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .role(UserRole.CLIENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createAppraiser(telegramId);

        assertEquals(UserRole.APPRAISER, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByTelegramId_WhenExists_ShouldReturnUser() {
        Long telegramId = 111222333L;
        User user = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        User result = userService.getUserByTelegramId(telegramId);

        assertEquals(user, result);
    }

    @Test
    void getUserByTelegramId_WhenNotExists_ShouldThrowException() {
        Long telegramId = 111222333L;
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByTelegramId(telegramId));
    }

    @Test
    void getUserById_WhenExists_ShouldReturnUser() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .telegramId(111222333L)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertEquals(user, result);
    }

    @Test
    void getUserById_WhenNotExists_ShouldThrowException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    }
}


