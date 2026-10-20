package com.game.shooting_game.settingScene;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class SettingManager extends JPanel implements Manager {
    private static final String component = "[TitleManager]";
    private static SettingManager instance;
    private final SettingSceneUI settingUI;

    private JButton goBackBtn;
    private JButton quitBtn;

    private SettingManager() {
        SettingManager.instance = this;
        settingUI = new SettingSceneUI();
    }

    public void initComponents() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/settingScene/goBackImage.png";
        goBackBtn = UIBuilder.createImageBtn(component, goBackImagePath, goBackBtnSize,
                "GO BACK");
        goBackBtn.setPreferredSize(
                new Dimension((int) goBackBtn.getWidth(), (int) goBackBtn.getHeight()));
        goBackBtn.addActionListener(e -> {
            SceneManager.getInstance().changeToPrevScene();
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, (int) (10 * ShootingGameMain.getScreenSizeAdj()), 0);
        gbc.gridy = 0;
        this.add(goBackBtn, gbc);

        // ゲーム終了ボタン(確認のあと、ウィンドウを閉じるときと同じ処理でデータを保存して終了)
        quitBtn = UIBuilder.createImageBtn(component, (BufferedImage) null, goBackBtnSize, "QUIT GAME");
        quitBtn.setPreferredSize(
                new Dimension((int) quitBtn.getWidth(), (int) quitBtn.getHeight()));
        quitBtn.addActionListener(e -> confirmQuit());
        gbc.gridy = 1;
        this.add(quitBtn, gbc);
    }

    /**
     * 終了の確認。「はい」ならウィンドウを閉じる処理(ShootingGameMainのwindowClosing: 保存して終了)を呼ぶ
     */
    private void confirmQuit() {
        int answer = JOptionPane.showConfirmDialog(this, "ゲームを終了しますか?", "QUIT GAME",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION)
            return;

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null)
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static SettingManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, SettingManager::new);
    }

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        settingUI.draw(g);
    }
}