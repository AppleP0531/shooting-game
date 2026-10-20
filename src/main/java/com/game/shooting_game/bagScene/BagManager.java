package com.game.shooting_game.bagScene;

import java.awt.Graphics;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagSceneUI.BagSceneUIData;
import com.game.shooting_game.bagScene.subPanels.OwnedResourcesPanel;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class BagManager extends JPanel implements Manager {
    private static final String component = "[BagManager]";
    private static BagManager instance;

    private final ShootingGameMain MAIN;
    private final BagSceneUI bagUI;
    private final OwnedResourcesPanel ownedResourcesPanel;

    private JButton goBackBtn;
    private JButton shopBtn;

    @Getter
    private Map<ResourceKinds, Integer> equipments;

    private BagManager() {
        MAIN = ShootingGameMain.getInstance();

        bagUI = new BagSceneUI();
        ownedResourcesPanel = new OwnedResourcesPanel();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(ownedResourcesPanel);

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/bagScene/goBackImage.png";
        goBackBtn = UIBuilder.createImageBtn(component, goBackImagePath, goBackBtnSize,
                "GO BACK");
        goBackBtn.setBounds(
                MAIN.getWidth() / 2 - (int) goBackBtn.getWidth(),
                MAIN.getHeight() - (int) goBackBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) goBackBtn.getWidth(), (int) goBackBtn.getHeight());
        goBackBtn.addActionListener(e -> {
            SceneManager.getInstance().changeToPrevScene();
        });
        this.add(goBackBtn);

        // ショップボタン
        Vector2 shopBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String shopImagePath = "/static/img/UI/bagScene/shopImage.png";
        shopBtn = UIBuilder.createImageBtn(component, shopImagePath, shopBtnSize, "Shop");
        shopBtn.setBounds(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - (int) shopBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) shopBtn.getWidth(), (int) shopBtn.getHeight());
        shopBtn.addActionListener(e -> {
            SceneManager.getInstance().changeScene(SceneType.SHOP);
        });
        this.add(shopBtn);

        ownedResourcesPanel.initComponents();
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
        equipments = DataManager.getUserData().getResources().getEquipments();
        ownedResourcesPanel.setup();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        BagSceneUIData uiData = new BagSceneUIData();
        bagUI.draw(g, uiData);
        ownedResourcesPanel.drawPanel(g);
    }
}
