package com.game.shooting_game.gameResultScene;

import java.awt.Graphics;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameResultScene.subPanels.RewardListPanel;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class GameResultManager extends JPanel implements Manager {
    private static final String component = "[GameResultManager]";
    private static GameResultManager instance;
    private final ShootingGameMain MAIN;
    private final GameResultSceneUI gameResultUI;
    private final RewardListPanel rewardListPanel;

    private JButton lobbyButton;
    private JButton replayButton;

    private GameResultManager() {
        GameResultManager.instance = this;
        MAIN = ShootingGameMain.getInstance();
        gameResultUI = new GameResultSceneUI();
        rewardListPanel = new RewardListPanel();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(rewardListPanel);

        // ロビー戻るボタン(ステージ選択画面に戻る)
        Vector2 lobbyButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/gameResultScene/lobbyImage.png";
        lobbyButton = UIBuilder.createImageButton(component, lobbyImagePath, lobbyButtonSize, "LOBBY");
        lobbyButton.setBounds(MAIN.getWidth() / 2 - (int) lobbyButton.getWidth(),
                MAIN.getHeight() - (int) lobbyButton.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyButton.getWidth(), (int) lobbyButton.getHeight());
        lobbyButton.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.LOBBY);
        });
        this.add(lobbyButton);

        // 再プレイボタン(ゲーム画面に遷移)
        Vector2 replayButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String replayImagePath = "/static/img/UI/gameResultScene/replayImage.png";
        replayButton = UIBuilder.createImageButton(component, replayImagePath, replayButtonSize, "REPLAY");
        replayButton.setBounds(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - (int) replayButton.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) replayButton.getWidth(), (int) replayButton.getHeight());
        replayButton.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.GAME);
        });
        this.add(replayButton);

        rewardListPanel.initComponents();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static GameResultManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, GameResultManager::new);
    }

    @Override
    public void setup() {
        rewardListPanel.setup();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        gameResultUI.draw(g);
        rewardListPanel.drawPanel(g);
    }
}