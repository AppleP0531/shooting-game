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
        setTitle("Login");

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton changeStateBtn = new JButton("Create Account");
        changeStateBtn.addActionListener(e -> {
            if (getTitle().equals("Login")) {
                setTitle("Create Account");
                changeStateBtn.setText("Create Account");
            } else if (getTitle().equals("Create Account")) {
                setTitle("Login");
                changeStateBtn.setText("Sign in");
            }

            usernameField.setText("");
            passwordField.setText("");

            revalidate();
            repaint();
        });
        panel.add(changeStateBtn);

        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            if (getTitle().equals("Login")) {
                loginBtn.setText("Login");

                if (userService.authenticate(usernameField.getText(), new String(passwordField.getPassword()))) {
                    successed = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (getTitle().equals("Create Account")) {
                loginBtn.setText("Create");

                if (usernameField.getText().isEmpty() || new String(passwordField.getPassword()).isEmpty())
                    JOptionPane.showMessageDialog(this, "Can not create empty account name or password", "Error",
                            JOptionPane.ERROR_MESSAGE);

                if (userService.registerUser(usernameField.getText(), new String(passwordField.getPassword()))) {
                    successed = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Account has already exists", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
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
