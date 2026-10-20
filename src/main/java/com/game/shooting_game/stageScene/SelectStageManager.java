package com.game.shooting_game.stageScene;

import java.awt.Graphics;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class SelectStageManager extends JPanel implements Manager {
    private static final String component = "[SelectStageManager]";
    private static SelectStageManager instance;

    private static enum StageModeButton {
        NORMAL("NORMAL MODE", "/static/img/UI/selectStageScene/normalModeImage.png", SceneType.NORMALSTAGE),
        INFINITY("INFINITY MODE", "/static/img/UI/selectStageScene/infinityModeImage.png", SceneType.INFINITYSTAGE),
        EXTRA_MODULE("MODULE", "/static/img/UI/selectStageScene/extraModeImage.png", SceneType.EXTRA_MODULE),
        EXTRA_BOSS("BOSS", "/static/img/UI/selectStageScene/extraModeImage.png", SceneType.EXTRA_BOSS),
        EXTRA_MATERIAL("MATERIAL", "/static/img/UI/selectStageScene/extraModeImage.png", SceneType.EXTRA_MATERIAL),
        EXTRA_SPECIAL("SPECIAL", "/static/img/UI/selectStageScene/extraModeImage.png", SceneType.EXTRA_SPECIAL);

        private final String label;
        private final String imagePath;
        private final SceneType sceneType;

        private StageModeButton(String label, String imagePath, SceneType sceneType) {
            this.label = label;
            this.imagePath = imagePath;
            this.sceneType = sceneType;
        }

        public String getLabel() {
            return label;
        }

        public String getImagePath() {
            return imagePath;
        }

        public SceneType getTargetScene() {
            return sceneType;
        }
    }

    private final ShootingGameMain MAIN;

    private JButton goBackBtn;
    private final Map<StageModeButton, JButton> modeBtnMap = new EnumMap<>(StageModeButton.class);

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

        // モードボタンを配列(enum)から自動生成
        Vector2 modeBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());

        StageModeButton[] modes = StageModeButton.values();
        for (int i = 0; i < modes.length; i++) {
            StageModeButton mode = modes[i];

            JButton btn = UIBuilder.createImageBtn(component, mode.getImagePath(), modeBtnSize, mode.getLabel());
            double y = 110 + i * 60;
            btn.setBounds(
                    MAIN.getWidth() / 2 - (int) btn.getWidth() / 2,
                    (int) (y * ShootingGameMain.getScreenSizeAdj()),
                    (int) btn.getWidth(), (int) btn.getHeight());
            btn.addActionListener(e -> {
                SceneManager.getInstance().changeScene(mode.getTargetScene());
            });

            modeBtnMap.put(mode, btn);
            this.add(btn);
        }
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
