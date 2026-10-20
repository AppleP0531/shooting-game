package com.game.shooting_game.settingScene;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.GameSettings;
import com.game.shooting_game.utils.Vector2;

public final class SettingManager extends JPanel implements Manager {
    private static final String component = "[TitleManager]";
    private static SettingManager instance;
    private final SettingSceneUI settingUI;

    private JButton goBackBtn;
    private JButton quitBtn;
    private final Map<Integer, JButton> fpsBtns = new LinkedHashMap<>();
    private final Map<Boolean, JButton> damageTextBtns = new LinkedHashMap<>();

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

        // FPSの選択(押すとすぐ反映して、端末の設定として保存する)
        gbc.gridy = 0;
        this.add(createFpsRow(), gbc);

        // ダメージ数字の表示(ON/OFF)
        gbc.gridy = 1;
        this.add(createDamageTextRow(), gbc);

        gbc.gridy = 2;
        this.add(goBackBtn, gbc);

        // ゲーム終了ボタン(確認のあと、ウィンドウを閉じるときと同じ処理でデータを保存して終了)
        quitBtn = UIBuilder.createImageBtn(component, (BufferedImage) null, goBackBtnSize, "QUIT GAME");
        quitBtn.setPreferredSize(
                new Dimension((int) quitBtn.getWidth(), (int) quitBtn.getHeight()));
        quitBtn.addActionListener(e -> confirmQuit());
        gbc.gridy = 3;
        this.add(quitBtn, gbc);
    }

    /**
     * FPSの行(「FPS」の文字と、選べる値のボタン)
     */
    private JPanel createFpsRow() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, (int) (6 * adj), 0));
        row.setOpaque(false);

        JLabel label = new JLabel("FPS");
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) (14 * adj)));
        row.add(label);

        for (int fps : GameSettings.FPS_CHOICES) {
            JButton btn = UIBuilder.createImageBtn(component, (BufferedImage) null,
                    new Vector2(52 * adj, 30 * adj), String.valueOf(fps));
            btn.setPreferredSize(new Dimension((int) (52 * adj), (int) (30 * adj)));
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.addActionListener(e -> {
                ShootingGameMain.getInstance().setFps(fps);
                GameSettings.setFps(fps);
                updateFpsButtons();
            });
            fpsBtns.put(fps, btn);
            row.add(btn);
        }
        updateFpsButtons();
        return row;
    }

    /**
     * ダメージ表記の行(「ダメージ表記」の文字と、ON / OFF のボタン)
     */
    private JPanel createDamageTextRow() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, (int) (6 * adj), 0));
        row.setOpaque(false);

        JLabel label = new JLabel("ダメージ表記");
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) (14 * adj)));
        row.add(label);

        for (boolean enabled : new boolean[] { true, false }) {
            JButton btn = UIBuilder.createImageBtn(component, (BufferedImage) null,
                    new Vector2(60 * adj, 30 * adj), enabled ? "ON" : "OFF");
            btn.setPreferredSize(new Dimension((int) (60 * adj), (int) (30 * adj)));
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.addActionListener(e -> {
                GameSettings.setDamageTextEnabled(enabled);
                updateDamageTextButtons();
            });
            damageTextBtns.put(enabled, btn);
            row.add(btn);
        }
        updateDamageTextButtons();
        return row;
    }

    /**
     * 今の設定のボタンを強調する
     */
    private void updateDamageTextButtons() {
        boolean current = GameSettings.isDamageTextEnabled();
        damageTextBtns.forEach((enabled, btn) -> {
            boolean selected = enabled == current;
            String name = enabled ? "ON" : "OFF";
            btn.setText(selected ? "[" + name + "]" : name);
            btn.setFont(btn.getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN));
        });
    }

    /**
     * 今のFPSのボタンを強調する(コマンドで変えたときも、画面を開くたびに合わせる)
     */
    private void updateFpsButtons() {
        int current = ShootingGameMain.getInstance().getFps();
        fpsBtns.forEach((fps, btn) -> {
            boolean selected = fps == current;
            btn.setText(selected ? "[" + fps + "]" : String.valueOf(fps));
            btn.setFont(btn.getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN));
        });
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
        updateFpsButtons();
        updateDamageTextButtons();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        settingUI.draw(g);
    }
}