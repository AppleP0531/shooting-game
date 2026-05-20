package com.game.shooting_game.bagScene.subPanels;

import java.awt.Graphics;
import java.util.Map;

import javax.swing.JPanel;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class OwnedResourcesPanel extends JPanel {
    private final ShootingGameMain MAIN;

    private Vector2 panelSize; // 報酬リストパネルの大きさ
    private Vector2 originPos; // ドラッグ開始時のパネルの位置
    private double scrollOffset = 0; // スクロール量
    private double maxScrollOffset = 0; // 最大スクロール量
    private int col = 4; // 一行の個数

    public OwnedResourcesPanel() {
        MAIN = ShootingGameMain.getInstance();
    }

    public void initComponents() {
        panelSize = new Vector2(
                380 * ShootingGameMain.getScreenSizeAdj(),
                MAIN.getHeight() * 3 / 4);
        setLayout(null);
        setOpaque(false);
        setBounds(MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        addMouseWheelListener(e -> {
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * 20, 0, maxScrollOffset);
        });
    }

    /**
     * 初期化
     */
    public void setup() {
        Map<ResourceKinds, Integer> resources = BagManager.getInstance().getEquipments();
        int rows = resources.size() / col;
        // スクロール制限
        maxScrollOffset = Math.max(0,
                rows * (100 * ShootingGameMain.getScreenSizeAdj() + 10 * ShootingGameMain.getScreenSizeAdj())
                        - panelSize.getY());
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void drawPanel(Graphics g) {
        // 基準点
        originPos = new Vector2(
                50 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());

        int index = 0;
        for (Map.Entry<ResourceKinds, Integer> entry : BagManager.getInstance().getEquipments().entrySet()) {
            ResourceKinds name = entry.getKey();
            int amount = entry.getValue();

            Vector2 posInd = new Vector2(
                    index % col,
                    (int) (index / col));
            Vector2 size = new Vector2(
                    80 * ShootingGameMain.getScreenSizeAdj(),
                    100 * ShootingGameMain.getScreenSizeAdj());
            double spacing = 10 * ShootingGameMain.getScreenSizeAdj();
            Vector2 pos = new Vector2(
                    originPos.getX() + (size.getX() + spacing) * posInd.getX(),
                    originPos.getY() + (size.getY() + spacing) * posInd.getY());
            UIBuilder.drawEquipmentsIcon(this, g, pos, size, name, amount);

            index++;
        }
    }
}
