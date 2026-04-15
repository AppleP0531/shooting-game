package src.data;

import src.utils.ANSI;
import src.utils.LogUtils;
import src.entity.UserEntity;
import src.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
     * @return
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
     */
    public void changePassword(String username, String newPassword) {
        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        // 新しいパスワードをハッシュ化して保存
        String hashedPassword = passwordEncoder.encode(newPassword);
        entity.setPassword(hashedPassword);

        userRepository.save(entity);
    }

    /**
     * ユーザーの削除
     * 
     * @param username
     * @param password forceを指定した場合強制削除
     */
    public void deleteUser(String username, String password) {
        userRepository.findByUsername(username).ifPresentOrElse(entity -> {
            // パスワードの認証
            if (!password.equals("force")) {
                if (!authenticate(username, password)) {
                    LogUtils.systemLog(
                            "[WARN]",
                            component,
                            "Password is not true");
                    return;
                }
            }

            userRepository.delete(entity);
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    "User '" + username + "' and all associated data have been deleted.");

            // 自身のデータだった場合削除して強制終了
            if (username.equals(DataManager.getUserData().getUsername()))
                System.exit(0);

        }, () -> {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Unknown username: " + ANSI.YELLOW + username + ANSI.RESET);
        });
    }
}
