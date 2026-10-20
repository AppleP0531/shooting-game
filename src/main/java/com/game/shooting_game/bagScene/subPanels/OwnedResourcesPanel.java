package com.game.shooting_game.bagScene.subPanels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceData;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;

public class OwnedResourcesPanel extends JPanel {
    private final ShootingGameMain MAIN;
    private final AnimationManager AM;
    private final ResourceManager RM;

    private Map<ResourceKinds, String> animationId = new HashMap<>(); // アニメーションId

    private Vector2 panelSize; // 報酬リストパネルの大きさ
    private Vector2 originPos; // ドラッグ開始時のパネルの位置
    private double scrollOffset = 0; // スクロール量
    private double maxScrollOffset = 0; // 最大スクロール量
    private int col = 4; // 一行の個数

    public OwnedResourcesPanel() {
        MAIN = ShootingGameMain.getInstance();
        AM = AnimationManager.getInstance();
        RM = ResourceManager.getInstance();
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
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * 20, -maxScrollOffset, 0);
            repaint();
        });
    }

    /**
     * 初期化
     */
    public void setup() {
        Map<ResourceKinds, Integer> resources = BagManager.getInstance().getEquipments();
        // コインは並べないので、行数には数えない
        int rows = (itemCount() + col - 1) / col;
        // スクロール制限
        maxScrollOffset = Math.max(0,
                20 * ShootingGameMain.getScreenSizeAdj() +
                        rows * (100 * ShootingGameMain.getScreenSizeAdj() + 10 * ShootingGameMain.getScreenSizeAdj())
                        - panelSize.getY());

        for (ResourceKinds resKind : resources.keySet()) {
            // コインは描画しない
            if (resKind.getResName() == ResourceName.COIN)
                continue;

            BufferedImage[] imageFrames = null;
            // 画像情報の取得
            Map<ResourceName, Map<Rarity, BufferedImage[]>> viewCache = RM.getResourceImage();
            if (viewCache != null) {
                ResourceName resName = resKind.getResName();
                Map<Rarity, BufferedImage[]> rarityMap = viewCache.get(resName);
                if (rarityMap != null)
                    imageFrames = rarityMap.get(resKind.getRarity());
            }

            ResourceData resData = RM.getResourceConfig()
                    .get(resKind.getResName())
                    .getResData()
                    .get(resKind.getRarity());

            if (resData != null) {
                double frameRate = resData.getFrameRate();
                double imageSize = 100 * ShootingGameMain.getScreenSizeAdj(); // 描画時に再調整入るためそれより大きめに設定
                animationId.put(resKind, AM.addAnimation(new Animation(imageFrames, imageSize, frameRate)));
            }
        }
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void drawPanel(Graphics g) {
        g.setClip(getX(), getY(), getWidth(), getHeight());

        // 基準点
        originPos = new Vector2(
                50 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj() + scrollOffset);

        int index = 0;
        // 並べ替えて表示
        LinkedHashMap<ResourceKinds, Integer> sourtedResources = UIBuilder
                .sortItemList(BagManager.getInstance().getEquipments());
        for (Map.Entry<ResourceKinds, Integer> entry : sourtedResources.entrySet()) {
            ResourceKinds resKind = entry.getKey();
            int amount = entry.getValue();

            // コインは描画しない
            if (resKind.getResName() == ResourceName.COIN)
                continue;

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
            UIBuilder.drawEquipmentsIcon(this, g, pos, size, animationId.get(resKind), resKind, amount);

            index++;
        }

        // 素材の後ろに空欄のフレームを並べる(最後の行を埋め、パネルの高さを埋めるところまで)
        double pitchY = 110 * ShootingGameMain.getScreenSizeAdj(); // 100(枠)+10(間隔)
        int minRows = (int) Math.ceil(getHeight() / pitchY);
        int slots = Math.max((index + col - 1) / col, minRows) * col;
        for (int i = index; i < slots; i++) {
            Vector2 pos = new Vector2(
                    originPos.getX() + (80 + 10) * ShootingGameMain.getScreenSizeAdj() * (i % col),
                    originPos.getY() + pitchY * (i / col));
            drawEmptySlot(g, pos);
        }

        g.setClip(null);
    }

    /**
     * 所持素材の数(コインは並べないので数えない)
     */
    private int itemCount() {
        int count = 0;
        for (ResourceKinds resKind : BagManager.getInstance().getEquipments().keySet())
            if (resKind.getResName() != ResourceName.COIN)
                count++;
        return count;
    }

    /**
     * 空欄のフレーム(素材の枠と同じ大きさの、暗い角丸の四角)
     *
     * @param pos 枠の中心(このパネルの中の座標)
     */
    private void drawEmptySlot(Graphics g, Vector2 pos) {
        Graphics2D g2 = (Graphics2D) g.create();
        double adj = ShootingGameMain.getScreenSizeAdj();
        Point p = SwingUtilities.convertPoint(this, (int) pos.getX(), (int) pos.getY(), MAIN);
        int size = (int) (80 * adj);
        int arc = (int) (10 * adj);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 14));
        g2.fillRoundRect(p.x - size / 2, p.y - size / 2, size, size, arc, arc);
        g2.setStroke(new BasicStroke(Math.max(1f, (float) (1.5 * adj))));
        g2.setColor(new Color(70, 215, 255, 55));
        g2.drawRoundRect(p.x - size / 2, p.y - size / 2, size, size, arc, arc);
        g2.dispose();
    }
}
