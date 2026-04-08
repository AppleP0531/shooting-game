package src.bagScene;

import src.SceneManager;
import src.SceneManager.SceneType;
import src.ResourceManager.ResourceKinds;
import src.ShootingGameMain;
import src.data.DataManager;
import src.utils.UIBuilder;
import src.utils.Vector2;
import src.bagScene.BagSceneUI.BagSceneUIData;
import src.interfaces.Manager;

import java.awt.Graphics;
import java.util.Map;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;

public final class BagManager extends JPanel implements Manager {
    private static final String component = "[BagManager]";
    private static BagManager instance;
    private final BagSceneUI bagUI;

    private final ShootingGameMain MAIN;

    private JButton goBackButton;
    private JButton shopButton;

    private Map<ResourceKinds, Integer> equipments;

    // コンストラクタ
    private BagManager() {
        MAIN = ShootingGameMain.getInstance();

        bagUI = new BagSceneUI();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "static/img/UI/bagScene/goBackImage.png";
        goBackButton = UIBuilder.createImageButton(component, goBackImagePath, goBackButtonSize,
                "GO BACK");
        goBackButton.setBounds(
                MAIN.getWidth() / 2 - (int) goBackButton.getWidth(),
                MAIN.getHeight() - (int) goBackButton.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) goBackButton.getWidth(), (int) goBackButton.getHeight());
        goBackButton.addActionListener(e -> {
            // 一つ前の画面に遷移
            SceneManager.getInstance().changeToPreviousScene();
        });
        this.add(goBackButton);

        // ショップボタン
        Vector2 shopButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String shopImagePath = "static/img/UI/bagScene/shopImage.png";
        shopButton = UIBuilder.createImageButton(component, shopImagePath, shopButtonSize, "Shop");
        shopButton.setBounds(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - (int) shopButton.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) shopButton.getWidth(), (int) shopButton.getHeight());
        shopButton.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.SHOP);
        });
        this.add(shopButton);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static BagManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, BagManager::new);
    }

    @Override
    public void setup() {
        // 入手済み資源リストの取得
        equipments = DataManager.getUserData().getResources().getEquipment();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        BagSceneUIData uiData = new BagSceneUIData(equipments);
        bagUI.draw(g, uiData);
    }
}
