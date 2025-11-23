package com.yhladkevych.appraise.adapter.persistence;

import com.yhladkevych.appraise.adapter.persistence.entity.UserEntity;
import com.yhladkevych.appraise.adapter.persistence.repository.JpaUserRepository;
import com.yhladkevych.appraise.domain.model.User;
import com.yhladkevych.appraise.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        UserEntity entity = user.getId() != null
                ? jpaUserRepository.findById(user.getId())
                .map(e -> {
                    e.setTelegramId(user.getTelegramId());
                    e.setRole(user.getRole());
                    e.setUpdatedAt(user.getUpdatedAt());
                    return e;
                })
                .orElse(UserEntity.fromDomain(user))
                : UserEntity.fromDomain(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        return jpaUserRepository.findByTelegramId(telegramId)
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toDomain);
    }
}


