package src.data;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class LoginDialog extends JDialog {
    private JTextField usernameField = new JTextField(20);
    private JPasswordField passwordField = new JPasswordField(20);
    private boolean successed = false;

    public LoginDialog(Frame parent, UserService userService) {
        super(parent, "Login", true);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            if (userService.authenticate(usernameField.getText(), new String(passwordField.getPassword()))) {
                successed = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(new JLabel(""));
        panel.add(loginBtn);

        getContentPane().add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * ログイン成功かどうかを返す
     * 
     * @return
     */
    public boolean isSuccessed() {
        return successed;
    }

    /**
     * ユーザー名を取得する
     * 
     * @return
     */
    public String getUsername() {
        return usernameField.getText();
    }
}
