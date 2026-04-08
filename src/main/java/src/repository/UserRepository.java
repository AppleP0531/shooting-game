package src.repository;

import src.entity.UserEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ユーザーデータのリポジトリインターフェース
 */
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