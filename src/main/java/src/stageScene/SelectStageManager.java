package src.stageScene;

import src.SceneManager;
import src.ShootingGameMain;
import src.SceneManager.SceneType;
import src.utils.Vector2;
import src.utils.UIBuilder;
import src.interfaces.Manager;

import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;

public final class SelectStageManager extends JPanel implements Manager {
        private static final String component = "[SelectStageManager]";
        private static SelectStageManager instance;

        private final ShootingGameMain MAIN;

        private JButton goBackButton;
        private JButton normalModeButton;
        private JButton infinityModeButton;

        // コンストラクタ
        private SelectStageManager() {
                MAIN = ShootingGameMain.getInstance();
        }

        public void initComponents() {
                setLayout(null);
                setOpaque(false);

                // 戻るボタン(一つ前の画面に遷移)
                Vector2 goBackButtonSize = new Vector2(
                                200 * ShootingGameMain.getScreenSizeAdj(),
                                50 * ShootingGameMain.getScreenSizeAdj());
                String goBackImagePath = "static/img/UI/selectStageScene/goBackImage.png";
                goBackButton = UIBuilder.createImageButton(component, goBackImagePath, goBackButtonSize,
                                "GO BACK");
                goBackButton.setBounds(
                                MAIN.getWidth() / 2 - (int) goBackButton.getWidth() / 2,
                                (int) (50 * ShootingGameMain.getScreenSizeAdj()),
                                (int) goBackButton.getWidth(), (int) goBackButton.getHeight());
                goBackButton.addActionListener(e -> {
                        // 一つ前の画面に遷移
                        SceneManager.getInstance().changeToPreviousScene();
                });
                this.add(goBackButton);

                // ノーマルモードボタン
                Vector2 normalModeButtonSize = new Vector2(
                                200 * ShootingGameMain.getScreenSizeAdj(),
                                50 * ShootingGameMain.getScreenSizeAdj());
                String normalModeImagePath = "static/img/UI/selectStageScene/normalModeImage.png";
                normalModeButton = UIBuilder.createImageButton(component, normalModeImagePath, normalModeButtonSize,
                                "NORMAL MODE");
                normalModeButton.setBounds(
                                MAIN.getWidth() / 2 - (int) normalModeButton.getWidth() / 2,
                                (int) (110 * ShootingGameMain.getScreenSizeAdj()),
                                (int) normalModeButton.getWidth(), (int) normalModeButton.getHeight());
                normalModeButton.addActionListener(e -> {
                        SceneManager.getInstance().changeScene(SceneType.NORMALSTAGE);
                });
                this.add(normalModeButton);

                // 無限モードボタン
                Vector2 infinityModeButtonSize = new Vector2(
                                200 * ShootingGameMain.getScreenSizeAdj(),
                                50 * ShootingGameMain.getScreenSizeAdj());
                String infinityModeImagePath = "static/img/UI/selectStageScene/infinityModeImage.png";
                infinityModeButton = UIBuilder.createImageButton(component, infinityModeImagePath,
                                infinityModeButtonSize,
                                "INFINITY MODE");
                infinityModeButton.setBounds(
                                MAIN.getWidth() / 2 - (int) infinityModeButton.getWidth() / 2,
                                (int) (170 * ShootingGameMain.getScreenSizeAdj()),
                                (int) infinityModeButton.getWidth(), (int) infinityModeButton.getHeight());
                infinityModeButton.addActionListener(e -> {
                        SceneManager.getInstance().changeScene(SceneType.INFINITYSTAGE);
                });
                this.add(infinityModeButton);
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
