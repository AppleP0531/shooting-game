package com.game.shooting_game.stageScene.infinity;

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

public final class InfinityStageManager extends JPanel implements Manager {
    private static final String component = "[InfinityStageManager]";
    private static InfinityStageManager instance;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;

    private JButton lobbyBtn;
    private JButton settingBtn;

    private InfinityStageManager() {
        InfinityStageManager.instance = this;

        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static InfinityStageManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, InfinityStageManager::new);
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        // ロビーボタン(ロビー画面に遷移)
        Vector2 lobbyBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/stageScene/infinity/lobbyImage.pnt";
        lobbyBtn = UIBuilder.createImageBtn(component, lobbyImagePath, lobbyBtnSize, "LOBBY");
        lobbyBtn.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyBtn.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyBtn.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyBtn.getWidth(), (int) lobbyBtn.getHeight());
        lobbyBtn.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        this.add(lobbyBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        settingBtn.setBounds(
                MAIN.getWidth() - 10 - (int) settingBtn.getWidth(),
                10,
                (int) settingBtn.getWidth(), (int) settingBtn.getHeight());
        settingBtn.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        this.add(settingBtn);
    }

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {

    }
}
