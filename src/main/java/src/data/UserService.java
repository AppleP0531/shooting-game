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
    public void registerUser(String username, String password) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);

        // パスワードをハッシュ化して保存
        String hashedPassword = passwordEncoder.encode(password);
        entity.setPassword(hashedPassword);

        userRepository.save(entity);
    }
}
