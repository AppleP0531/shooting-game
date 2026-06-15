package com.game.shooting_game.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.game.shooting_game.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * ユーザー名でユーザーを検索
     * 
     * @param username
     * @return
     */
    Optional<UserEntity> findByUsername(String username);
}