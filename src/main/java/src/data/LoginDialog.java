package src.data;

import java.awt.Frame;
import java.awt.Color;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;

import src.utils.ANSI;
import src.utils.LogUtils;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class LoginDialog extends JDialog {
    private final static String component = "[LoginDialog]";
    private JTextField usernameField = new JTextField(20);
    private JPasswordField passwordField = new JPasswordField(20);

    public LoginDialog(Frame parent, UserService userService) {
        setTitle("Login");

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton changeStateBtn = new JButton("Create Account");
        JButton loginBtn = new JButton("Login");

        changeStateBtn.addActionListener(e -> {
            if (getTitle().equals("Login")) {
                setTitle("Create Account");
                loginBtn.setText("Create");
                changeStateBtn.setText("Sign in");
            } else if (getTitle().equals("Create Account")) {
                setTitle("Login");
                loginBtn.setText("Login");
                changeStateBtn.setText("Create Account");
            }

            usernameField.setText("");
            passwordField.setText("");

            revalidate();
            repaint();
        });
        changeStateBtn.setContentAreaFilled(false);
        changeStateBtn.setBorderPainted(false);
        changeStateBtn.setFocusPainted(false);
        changeStateBtn.setMargin(new Insets(0, 0, 0, 0));
        changeStateBtn.setForeground(Color.BLUE);
        panel.add(changeStateBtn);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (getTitle().equals("Login")) {
                if (userService.authenticate(username, password)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            ANSI.GREEN + "Login successful." + ANSI.RESET);
                    DataManager.load(username, "login");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (getTitle().equals("Create Account")) {
                if (username.equals("") || password.equals(""))
                    JOptionPane.showMessageDialog(this, "Can not create empty account name or password", "Error",
                            JOptionPane.ERROR_MESSAGE);

                if (userService.registerUser(username, password)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            ANSI.GREEN + "Login successful." + ANSI.RESET);
                    DataManager.load(username, "create");
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
}
