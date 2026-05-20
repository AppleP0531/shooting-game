package com.game.shooting_game.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.game.shooting_game.entity.UserEntity;
import com.game.shooting_game.repository.UserRepository;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

@Service
public class UserService {
    private static final String component = "[UserService]";
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;

    /**
     * ユーザーの登録
     * 
     * @param username
     * @param password
     * @return
     */
    public boolean registerUser(String username, String password) {
        UserEntity entity = userRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            entity = new UserEntity();
            entity.setUsername(username);

            // パスワードをハッシュ化して保存
            String hashedPassword = passwordEncoder.encode(password);
            entity.setPassword(hashedPassword);

            userRepository.save(entity);

            return true;
        }
        return false;
    }

    /**
     * ユーザーの認証
     * 
     * @param username
     * @param password
     * @return 認証成功したかどうか
     */
    public boolean authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    /**
     * ユーザーのパスワード変更
     * 
     * @param username
     * @param newPassword
     * @return 変更成功したかどうか
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        UserEntity entity = userRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            return false;
        }
        // パスワードの認証
        if (!authenticate(username, oldPassword)) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Password is not true");
            return false;
        }

        // 新しいパスワードをハッシュ化して保存
        String hashedPassword = passwordEncoder.encode(newPassword);
        entity.setPassword(hashedPassword);

        userRepository.save(entity);
        return true;
    }

    /**
     * ユーザーのパスワード強制変更
     * 
     * @param username
     * @param newPassword
     * @return 変更成功したかどうか
     */
    public boolean forceChangePassword(String username, String newPassword) {
        UserEntity entity = userRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            return false;
        }

        // 新しいパスワードをハッシュ化して保存
        String hashedPassword = passwordEncoder.encode(newPassword);
        entity.setPassword(hashedPassword);

        userRepository.save(entity);
        return true;
    }

    /**
     * ユーザーの削除
     * 
     * @param username
     * @param password
     * @return 削除成功したかどうか
     */
    public boolean deleteUser(String username, String password) {
        UserEntity entity = userRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Unknown username: " + ANSI.YELLOW + username + ANSI.RESET);
            return false;
        }

        // パスワードの認証
        if (!authenticate(username, password)) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Password is not true");
            return false;
        }

        userRepository.delete(entity);
        LogUtils.systemLog(
                "[INFO]",
                component,
                "User '" + username + "' and all associated data have been deleted.");

        // 自身のデータだった場合削除して強制終了
        if (username.equals(DataManager.getUserData().getUsername()))
            System.exit(0);

        return true;
    }

    /**
     * ユーザーの強制削除
     * 
     * @param username
     * @return 削除成功したかどうか
     */
    public boolean forceDeleteUser(String username) {
        UserEntity entity = userRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Unknown username: " + ANSI.YELLOW + username + ANSI.RESET);
            return false;
        }

        userRepository.delete(entity);
        LogUtils.systemLog(
                "[INFO]",
                component,
                "User '" + username + "' and all associated data have been deleted.");

        return true;
    }
}
