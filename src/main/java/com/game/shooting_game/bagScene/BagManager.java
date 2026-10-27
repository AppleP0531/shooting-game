package com.game.shooting_game.bagScene;

import java.awt.Graphics;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.bagScene.BagSceneUI.BagSceneUIData;
import com.game.shooting_game.bagScene.subPanels.OwnedResourcesPanel;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.unlock.UnlockService;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;

public final class BagManager extends JPanel implements Manager {
    private static final String component = "[BagManager]";
    private static BagManager instance;

    private final BagSceneUI bagUI;
    private final OwnedResourcesPanel ownedResourcesPanel;

    private JButton goBackBtn;

    @Getter
    private Map<ResourceKinds, Integer> equipments;

    private BagManager() {
        bagUI = new BagSceneUI();
        ownedResourcesPanel = new OwnedResourcesPanel();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(ownedResourcesPanel);

        // 戻るボタン(一つ前の画面に遷移)
        goBackBtn = UIBuilder.createBackBtn(component, () -> SceneManager.getInstance().changeToPrevScene());
        this.add(goBackBtn);

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
        UnlockService.refreshAll();
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
        com.game.shooting_game.ui.SceneBars.draw(g, com.game.shooting_game.ui.SceneBars.TOP, 56);
        bagUI.draw(g, uiData);
        ownedResourcesPanel.drawPanel(g);
    }
}
