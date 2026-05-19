package com.game.shooting_game.data;

import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.game.shooting_game.CommandGlassPane;
import com.game.shooting_game.ShootingGameMain;

import javax.swing.JPasswordField;

public class LoginPanel extends JPanel {
    private final static String component = "[LoginPanel]";
    private String state = "Login";
    private JTextField usernameField = new JTextField(20);
    private JPasswordField passwordField = new JPasswordField(20);

    private Vector2 panelSize;

    public LoginPanel(JPanel parent, UserService userService, CommandGlassPane glassPane) {
        setLayout(null);

        panelSize = new Vector2(
                300 * ShootingGameMain.getScreenSizeAdj(),
                200 * ShootingGameMain.getScreenSizeAdj());

        // 入力フィールド
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (30 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (10 * ShootingGameMain.getScreenSizeAdj()));
        add(usernameLabel);
        usernameField.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (40 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (20 * ShootingGameMain.getScreenSizeAdj()));
        add(usernameField);
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (70 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (10 * ShootingGameMain.getScreenSizeAdj()));
        add(passwordLabel);
        passwordField.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (80 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (20 * ShootingGameMain.getScreenSizeAdj()));
        add(passwordField);

        // ログイン、作成ボタン
        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (120 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (20 * ShootingGameMain.getScreenSizeAdj()));
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (state.equals("Login")) {
                if (userService.authenticate(username, password)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            ANSI.GREEN + "Login successful." + ANSI.RESET);
                    DataManager.load(username, "login");

                    // adminの場合アドミン権限を付与
                    if (username.equals("admin"))
                        glassPane.setAdminMode(true);

                    UIBuilder.closePanel(this);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (state.equals("Create Account")) {
                if (username.equals("") || password.equals("")) {
                    JOptionPane.showMessageDialog(this, "Can not create empty account name or password", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (userService.registerUser(username, password)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            ANSI.GREEN + "Login successful." + ANSI.RESET);
                    DataManager.load(username, "create");

                    UIBuilder.closePanel(this);
                } else {
                    JOptionPane.showMessageDialog(this, "Account has already exists", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
        add(loginBtn);

        // ログイン、アカウント作成切り替えボタン
        JButton changeStateBtn = new JButton("Create Account");
        changeStateBtn.setBounds(
                (int) (panelSize.getX() / 2 - 100 * ShootingGameMain.getScreenSizeAdj()),
                (int) (150 * ShootingGameMain.getScreenSizeAdj()),
                (int) (200 * ShootingGameMain.getScreenSizeAdj()),
                (int) (20 * ShootingGameMain.getScreenSizeAdj()));

        changeStateBtn.addActionListener(e -> {
            if (state.equals("Login")) {
                state = "Create Account";
                loginBtn.setText("Create");
                changeStateBtn.setText("Sign in");

                // 開きなおす
                UIBuilder.closePanel(this);
                showLoginPanel();
            } else if (state.equals("Create Account")) {
                state = "Login";
                loginBtn.setText("Login");
                changeStateBtn.setText("Create Account");

                // 開きなおす
                UIBuilder.closePanel(this);
                showLoginPanel();
            }
        });
        changeStateBtn.setContentAreaFilled(false);
        changeStateBtn.setBorderPainted(false);
        changeStateBtn.setFocusPainted(false);
        changeStateBtn.setMargin(new Insets(0, 0, 0, 0));
        changeStateBtn.setForeground(new Color(89, 150, 251));
        add(changeStateBtn);

        parent.add(this);
    }

    /**
     * ログインパネルを表示する
     */
    public void showLoginPanel() {
        ShootingGameMain MAIN = ShootingGameMain.getInstance();
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        UIBuilder.openPanel(this, state, true);
    }
}
