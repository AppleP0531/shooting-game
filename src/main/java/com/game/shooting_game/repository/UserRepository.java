package com.game.shooting_game.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.game.shooting_game.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * ユーザー名でユーザーを検索
     * 
     * @param username
     * @return
     */
    Optional<UserEntity> findByUsername(String username);
}