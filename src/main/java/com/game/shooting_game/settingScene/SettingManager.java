package com.game.shooting_game.settingScene;

import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;

import javax.swing.JButton;

public final class SettingManager extends JPanel implements Manager {
    private static final String component = "[TitleManager]";
    private static SettingManager instance;
    private final SettingSceneUI settingUI;

    private JButton goBackButton;

    private SettingManager() {
        SettingManager.instance = this;
        settingUI = new SettingSceneUI();
    }

    public void initComponents() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/settingScene/goBackImage.png";
        goBackButton = UIBuilder.createImageButton(component, goBackImagePath, goBackButtonSize,
                "GO BACK");
        goBackButton.setPreferredSize(
                new Dimension((int) goBackButton.getWidth(), (int) goBackButton.getHeight()));
        goBackButton.addActionListener(e -> {
            // 一つ前の画面に遷移
            SceneManager.getInstance().changeToPreviousScene();
        });
        this.add(goBackButton);
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