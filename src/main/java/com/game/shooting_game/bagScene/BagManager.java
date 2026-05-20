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
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class BagManager extends JPanel implements Manager {
    private static final String component = "[BagManager]";
    private static BagManager instance;

    private final ShootingGameMain MAIN;
    private final BagSceneUI bagUI;
    private final OwnedResourcesPanel ownedResourcesPanel;

    private JButton goBackButton;
    private JButton shopButton;

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
        Vector2 goBackButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/bagScene/goBackImage.png";
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
        String shopImagePath = "/static/img/UI/bagScene/shopImage.png";
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
        BagSceneUIData uiData = new BagSceneUIData(equipments);
        bagUI.draw(g, uiData);
        ownedResourcesPanel.drawPanel(g);
    }
}
