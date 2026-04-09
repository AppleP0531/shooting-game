package src.testScene;

import src.gameScene.GameManager;
import src.gameScene.item.ItemManager;
import src.gameScene.ship.ShipManager;
import src.gameScene.stage.StageManager;
import src.gameScene.bullet.BulletManager;
import src.interfaces.Manager;

import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 * テストシーン管理クラス
 */
public final class TestManager extends JPanel implements Manager {
    private static TestManager instance;
    private final TestSceneUI testUI;

    private final GameManager GM;
    private final StageManager STAGE_M;
    private final ShipManager SHIP_M;
    private final BulletManager BM;
    private final ItemManager IM;

    private final List<Manager> managers = new ArrayList<>();

    private TestManager() {
        TestManager.instance = this;
        testUI = new TestSceneUI();

        GM = GameManager.getInstance();
        STAGE_M = StageManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        BM = BulletManager.getInstance();
        IM = ItemManager.getInstance();

        managers.add(STAGE_M);
        managers.add(BM);
        managers.add(SHIP_M);
        managers.add(IM);
    }

    public void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(false);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static TestManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, TestManager::new);
    }

    @Override
    public void setup() {
        STAGE_M.setStageId(0);

        GM.setup();
    }

    @Override
    public void update(double dt) {
        GM.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        testUI.draw(g);
        GM.draw(g);
    }
}
