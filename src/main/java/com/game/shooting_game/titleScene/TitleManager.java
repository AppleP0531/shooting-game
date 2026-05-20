package com.game.shooting_game.titleScene;

import java.awt.Graphics;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class TitleManager extends JPanel implements Manager {
    private static final String component = "[TitleManager]";
    private static TitleManager instance;
    private final TitleSceneUI titleUI;

    private final ShootingGameMain MAIN;
    private JButton startButton;

    private TitleManager() {
        TitleManager.instance = this;
        titleUI = new TitleSceneUI();

        MAIN = ShootingGameMain.getInstance();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        // ゲームスタートボタン(ロビー画面に遷移)
        Vector2 startButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String startImagePath = "/static/img/UI/titleScene/startGameImage.png";
        startButton = UIBuilder.createImageButton(component, startImagePath, startButtonSize, "GAME START");
        startButton.setBounds(
                MAIN.getWidth() / 2 - startButton.getWidth() / 2,
                MAIN.getHeight() / 2 - startButton.getHeight() / 2,
                (int) startButton.getWidth(),
                (int) startButton.getHeight());
        startButton.addActionListener(e -> SceneManager.getInstance().changeScene(SceneType.LOBBY));
        this.add(startButton);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static TitleManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, TitleManager::new);
    }

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        titleUI.draw(g);
    }
}