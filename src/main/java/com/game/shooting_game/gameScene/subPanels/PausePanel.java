package com.game.shooting_game.gameScene.subPanels;

import java.awt.Color;
import java.awt.event.MouseAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class PausePanel extends JPanel {
    private static final String component = "[pausePanel]";

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final GameManager GM;

    private Vector2 panelSize; // ステージ詳細パネルの大きさ
    private int spacing; // 外周のスペース

    private JButton quitBtn;
    private JButton resumeBtn;

    public PausePanel() {
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        GM = GameManager.getInstance();
    }

    public void initComponents() {
        setLayout(null);
        setBackground(new Color(0, 0, 0, 220));
        setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        setVisible(false);

        panelSize = new Vector2(
                250 * ShootingGameMain.getScreenSizeAdj(),
                300 * ShootingGameMain.getScreenSizeAdj());
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());
        spacing = 5;

        // 再開ボタン(ゲーム画面を再開)
        Vector2 resumeBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String resumeImagePath = "/static/img/UI/gameScene/resume.png";
        resumeBtn = UIBuilder.createImageBtn(component, resumeImagePath, resumeBtnSize, "RESUME");
        resumeBtn.setBounds(
                (int) panelSize.getX() - (int) resumeBtn.getWidth() - spacing,
                (int) panelSize.getY() - (int) resumeBtn.getHeight() - spacing,
                (int) resumeBtn.getWidth(), (int) resumeBtn.getHeight());
        resumeBtn.addActionListener(e -> {
            UIBuilder.closePanel(this);
            GM.resume(); // 3・2・1を数えてから再開する
            // 再開ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.YELLOW + "Game Resumed" + ANSI.RESET);
        });
        this.add(resumeBtn);

        // 戻るボタン(ロビーに戻る)
        Vector2 quitBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String skipStageImagePath = "/static/img/UI/gameScene/quit.png";
        quitBtn = UIBuilder.createImageBtn(component, skipStageImagePath, quitBtnSize, "QUIT");
        quitBtn.setBounds(
                spacing,
                (int) panelSize.getY() - (int) quitBtn.getHeight() - spacing,
                (int) quitBtn.getWidth(), (int) quitBtn.getHeight());
        quitBtn.addActionListener(e -> {
            UIBuilder.closePanel(this);
            GM.leave(); // 画面を暗くしてから、ステージ選択へ戻る
        });
        this.add(quitBtn);

        // 背後へのクリックを阻止
        MouseAdapter blocker = new MouseAdapter() {
        };
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
    }

    /**
     * パネルの表示
     */
    public void showPausePanel() {
        // ポーズログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + "Game Paused" + ANSI.RESET);

        UIBuilder.openPanel(this, "Pause", true, () -> {
            UIBuilder.closePanel(this);
            GM.resume();
        });
    }
}
