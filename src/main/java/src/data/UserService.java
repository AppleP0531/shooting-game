package src.data;

import src.entity.UserEntity;
import src.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
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
}
