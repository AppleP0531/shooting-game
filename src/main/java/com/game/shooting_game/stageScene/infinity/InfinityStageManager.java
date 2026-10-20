package com.game.shooting_game.stageScene.infinity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.stage.EndlessWaves;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class InfinityStageManager extends JPanel implements Manager {
    private static final String component = "[InfinityStageManager]";
    private static InfinityStageManager instance;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;

    private JButton lobbyBtn;
    private JButton settingBtn;
    private JButton startBtn;
    private JButton waveDownBtn; // 開始ウェーブを下げる
    private JButton waveUpBtn; // 開始ウェーブを上げる
    private int startWave = 1; // 選んでいる開始ウェーブ

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
        String lobbyImagePath = "/static/img/UI/stageScene/infinity/lobbyImage.png";
        lobbyBtn = UIBuilder.createImageBtn(component, lobbyImagePath, lobbyBtnSize, "LOBBY");
        lobbyBtn.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyBtn.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyBtn.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyBtn.getWidth(), (int) lobbyBtn.getHeight());
        lobbyBtn.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        this.add(lobbyBtn);

        // 開始ボタン(無限モードを始める)
        Vector2 startSize = new Vector2(150 * ShootingGameMain.getScreenSizeAdj(),
                70 * ShootingGameMain.getScreenSizeAdj());
        startBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/startStage.png", startSize, "START");
        startBtn.setBounds(MAIN.getWidth() / 2 - (int) startBtn.getWidth() / 2,
                (int) (MAIN.getHeight() * 0.55), (int) startBtn.getWidth(), (int) startBtn.getHeight());
        startBtn.addActionListener(e -> {
            StageManager.getInstance().getStage().setStageId(EndlessWaves.STAGE_ID);
            StageManager.getInstance().getStage().setStartWave(startWave);
            SM.changeScene(SceneType.GAME);
        });
        this.add(startBtn);

        // 開始ウェーブの選択(スキル「助走」で選べる範囲が広がる。矢印で上げ下げする)
        int arrow = (int) (36 * ShootingGameMain.getScreenSizeAdj());
        int waveY = startBtn.getY() + startBtn.getHeight() + (int) (14 * ShootingGameMain.getScreenSizeAdj());
        waveDownBtn = UIBuilder.createImageBtn(component, "/static/img/UI/backArrow.png", new Vector2(arrow, arrow), "◀");
        waveDownBtn.setBounds(MAIN.getWidth() / 2 - (int) (96 * ShootingGameMain.getScreenSizeAdj()) - arrow / 2, waveY, arrow, arrow);
        waveDownBtn.addActionListener(e -> changeStartWave(-1));
        this.add(waveDownBtn);
        waveUpBtn = UIBuilder.createImageBtn(component, "/static/img/UI/forwardArrow.png", new Vector2(arrow, arrow), "▶");
        waveUpBtn.setBounds(MAIN.getWidth() / 2 + (int) (96 * ShootingGameMain.getScreenSizeAdj()) - arrow / 2, waveY, arrow, arrow);
        waveUpBtn.addActionListener(e -> changeStartWave(1));
        this.add(waveUpBtn);

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
        startWave = Math.max(1, Math.min(startWave, maxStartWave()));
        refreshWaveButtons();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int cx = MAIN.getWidth() / 2;
        int best = DataManager.getUserData().getProgress().getHighScores().getOrDefault(EndlessWaves.BEST_WAVE_KEY, 0);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, (int) (22 * adj))));
        drawCentered(g, "INFINITY", cx, (int) (60 * adj));

        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, (int) (12 * adj))));
        g.setColor(new Color(190, 220, 255));
        String[] lines = {
                "ゲームオーバーまで続くステージ。ウェーブが進むほど敵が強くなる。",
                "10ウェーブごとにコインボスが出る(ダメージを与えるたびにコインを落とす。",
                "ゲージを1本壊すか、時間が経つと撤収する)。",
                "到達したウェーブに応じて、基板・素材・モジュールが手に入る。",
                "(5ウェーブごとに基板、25ウェーブごとにモジュール)" };
        int y = (int) (95 * adj);
        for (String s : lines) {
            drawCentered(g, s, cx, y);
            y += (int) (18 * adj);
        }

        g.setColor(Color.YELLOW);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, (int) (18 * adj))));
        drawCentered(g, "最高到達ウェーブ  " + best, cx, (int) (MAIN.getHeight() * 0.5));

        // 開始ウェーブ(矢印の間に出す)
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, (int) (16 * adj))));
        int textY = waveDownBtn.getY() + waveDownBtn.getHeight() / 2 - (int) (2 * adj);
        drawCentered(g, "開始ウェーブ  " + startWave, cx, textY);
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (11 * adj))));
        g.setColor(new Color(190, 220, 255));
        drawCentered(g, "選べるのは 1〜" + maxStartWave() + "(スキル「助走」で増える)", cx, textY + (int) (18 * adj));
    }

    /**
     * 選べる開始ウェーブの最大(スキル「助走」で増え、これまでに到達したウェーブを超えない)
     */
    private int maxStartWave() {
        int best = DataManager.getUserData().getProgress().getHighScores().getOrDefault(EndlessWaves.BEST_WAVE_KEY, 0);
        int skill = 1 + (int) com.game.shooting_game.domain.skill.SkillService.getBonus("ENDLESS_SKIP");
        return Math.max(1, Math.min(skill, best + 1));
    }

    private void changeStartWave(int delta) {
        startWave = Math.max(1, Math.min(maxStartWave(), startWave + delta));
        refreshWaveButtons();
        repaint();
    }

    private void refreshWaveButtons() {
        waveDownBtn.setEnabled(startWave > 1);
        waveUpBtn.setEnabled(startWave < maxStartWave());
    }

    private static void drawCentered(Graphics g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}
