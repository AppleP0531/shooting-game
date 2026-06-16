package com.game.shooting_game.stageScene;

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

public final class SelectStageManager extends JPanel implements Manager {
    private static final String component = "[SelectStageManager]";
    private static SelectStageManager instance;

    private final ShootingGameMain MAIN;

    private JButton goBackBtn;
    private JButton normalModeBtn;
    private JButton infinityModeBtn;

    private SelectStageManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/selectStageScene/goBackImage.png";
        goBackBtn = UIBuilder.createImageBtn(component, goBackImagePath, goBackBtnSize,
                "GO BACK");
        goBackBtn.setBounds(
                MAIN.getWidth() / 2 - (int) goBackBtn.getWidth() / 2,
                (int) (50 * ShootingGameMain.getScreenSizeAdj()),
                (int) goBackBtn.getWidth(), (int) goBackBtn.getHeight());
        goBackBtn.addActionListener(e -> {
            SceneManager.getInstance().changeToPrevScene();
        });
        this.add(goBackBtn);

        // ノーマルモードボタン
        Vector2 normalModeBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String normalModeImagePath = "/static/img/UI/selectStageScene/normalModeImage.png";
        normalModeBtn = UIBuilder.createImageBtn(component, normalModeImagePath, normalModeBtnSize,
                "NORMAL MODE");
        normalModeBtn.setBounds(
                MAIN.getWidth() / 2 - (int) normalModeBtn.getWidth() / 2,
                (int) (110 * ShootingGameMain.getScreenSizeAdj()),
                (int) normalModeBtn.getWidth(), (int) normalModeBtn.getHeight());
        normalModeBtn.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.NORMALSTAGE);
        });
        this.add(normalModeBtn);

        // 無限モードボタン
        Vector2 infinityModeBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String infinityModeImagePath = "/static/img/UI/selectStageScene/infinityModeImage.png";
        infinityModeBtn = UIBuilder.createImageBtn(component, infinityModeImagePath,
                infinityModeBtnSize,
                "INFINITY MODE");
        infinityModeBtn.setBounds(
                MAIN.getWidth() / 2 - (int) infinityModeBtn.getWidth() / 2,
                (int) (170 * ShootingGameMain.getScreenSizeAdj()),
                (int) infinityModeBtn.getWidth(), (int) infinityModeBtn.getHeight());
        infinityModeBtn.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.INFINITYSTAGE);
        });
        this.add(infinityModeBtn);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static SelectStageManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, SelectStageManager::new);
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
