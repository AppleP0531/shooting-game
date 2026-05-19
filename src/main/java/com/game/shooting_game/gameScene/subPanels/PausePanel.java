package com.game.shooting_game.gameScene.subPanels;

import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.gameScene.GameManager;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.SceneManager.SceneType;

public class PausePanel extends JPanel {
    private static final String component = "[pausePanel]";

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final GameManager GM;

    private Vector2 panelSize; // ステージ詳細パネルの大きさ
    private int spacing; // 外周のスペース

    private JButton quitButton;
    private JButton resumeButton;

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
        spacing = 5;

        // 再開ボタン(ゲーム画面を再開)
        Vector2 resumeButtonSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String resumeImagePath = "/static/img/UI/gameScene/resume.png";
        resumeButton = UIBuilder.createImageButton(component, resumeImagePath, resumeButtonSize, "RESUME");
        resumeButton.setBounds(
                (int) panelSize.getX() - (int) resumeButton.getWidth() - spacing,
                (int) panelSize.getY() - (int) resumeButton.getHeight() - spacing,
                (int) resumeButton.getWidth(), (int) resumeButton.getHeight());
        resumeButton.addActionListener(e -> {
            GM.setPause(false);
            setVisible(false);
            // 再開ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.YELLOW + "Game Resumed" + ANSI.RESET);
        });
        this.add(resumeButton);

        // 戻るボタン(ロビーに戻る)
        Vector2 quitButtonSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String skipStageImagePath = "/static/img/UI/gameScene/quit.png";
        quitButton = UIBuilder.createImageButton(component, skipStageImagePath, quitButtonSize, "QUIT");
        quitButton.setBounds(
                spacing,
                (int) panelSize.getY() - (int) quitButton.getHeight() - spacing,
                (int) quitButton.getWidth(), (int) quitButton.getHeight());
        quitButton.addActionListener(e -> {
            GM.setPause(false);
            setVisible(false);
            SM.changeScene(SceneType.NORMALSTAGE);
        });
        this.add(quitButton);

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
        setVisible(true);

        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        // ポーズログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + "Game Paused" + ANSI.RESET);

        // 最前面に持ってくる
        if (getParent() != null)
            getParent().setComponentZOrder(this, 0);
    }
}
