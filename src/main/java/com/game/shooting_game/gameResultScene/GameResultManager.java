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

    private JButton goBackBtn;
    private JButton replayBtn;

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

        // 戻るボタン(ステージ選択画面に戻る)
        Vector2 goBackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/gameResultScene/goBackImage.png";
        goBackBtn = UIBuilder.createImageBtn(component, goBackImagePath, goBackBtnSize, "SELECT STAGE");
        goBackBtn.setBounds(MAIN.getWidth() / 2 - (int) goBackBtn.getWidth(),
                MAIN.getHeight() - (int) goBackBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) goBackBtn.getWidth(), (int) goBackBtn.getHeight());
        goBackBtn.addActionListener(e -> {
            SceneManager.getInstance().changeToPrevScene();
        });
        this.add(goBackBtn);

        // 再プレイボタン(ゲーム画面に遷移)
        Vector2 replayBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String replayImagePath = "/static/img/UI/gameResultScene/replayImage.png";
        replayBtn = UIBuilder.createImageBtn(component, replayImagePath, replayBtnSize, "REPLAY");
        replayBtn.setBounds(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - (int) replayBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) replayBtn.getWidth(), (int) replayBtn.getHeight());
        replayBtn.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.GAME);
        });
        this.add(replayBtn);

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