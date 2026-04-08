package src.gameResultScene;

import src.SceneManager;
import src.SceneManager.SceneType;
import src.ShootingGameMain;
import src.interfaces.Manager;
import src.utils.UIBuilder;
import src.utils.Vector2;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;

/**
 * ゲームリザルトシーン管理クラス
 */
public final class GameResultManager extends JPanel implements Manager {
    private static final String component = "[GameResultManager]";
    private static GameResultManager instance;
    private final GameResultSceneUI gameResultUI;

    private JButton lobbyButton;
    private JButton replayButton;

    // コンストラクタ
    private GameResultManager() {
        GameResultManager.instance = this;
        gameResultUI = new GameResultSceneUI();
    }

    public void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        // ロビー戻るボタン(ステージ選択画面に戻る)
        Vector2 lobbyButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/gameResultScene/lobbyImage.png";
        lobbyButton = UIBuilder.createImageButton(component, lobbyImagePath, lobbyButtonSize, "LOBBY");
        lobbyButton.setPreferredSize(
                new Dimension((int) lobbyButton.getWidth(), (int) lobbyButton.getHeight()));
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
        replayButton.setPreferredSize(new Dimension((int) replayButton.getWidth(), (int) replayButton.getHeight()));
        replayButton.addActionListener(e -> SceneManager.getInstance().changeScene(SceneType.GAME));
        this.add(replayButton);
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

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        gameResultUI.draw(g);
    }
}