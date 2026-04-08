package src.stageScene.infinity;

import src.SceneManager;
import src.ShootingGameMain;
import src.SceneManager.SceneType;
import src.interfaces.Manager;
import src.utils.Vector2;
import src.utils.UIBuilder;

import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;

/**
 * インフィニティステージシーン管理クラス
 */
public final class InfinityStageManager extends JPanel implements Manager {
    private static final String component = "[InfinityStageManager]";
    private static InfinityStageManager instance;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;

    private JButton lobbyButton;
    private JButton settingButton;

    // コンストラクタ
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
        Vector2 lobbyButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/stageScene/infinity/lobbyImage.pnt";
        lobbyButton = UIBuilder.createImageButton(component, lobbyImagePath, lobbyButtonSize, "LOBBY");
        lobbyButton.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyButton.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyButton.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyButton.getWidth(), (int) lobbyButton.getHeight());
        lobbyButton.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        this.add(lobbyButton);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingButtonSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingButton = UIBuilder.createImageButton(component, settingImagePath, settingButtonSize, "SETTING");
        settingButton.setBounds(
                MAIN.getWidth() - 10 - (int) settingButton.getWidth(),
                10,
                (int) settingButton.getWidth(), (int) settingButton.getHeight());
        settingButton.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        this.add(settingButton);
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
