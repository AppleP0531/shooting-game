package com.game.shooting_game.gameResultScene.subPanels;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceData;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.model.StageRewards;
import com.game.shooting_game.domain.module.Module;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;

public class RewardListPanel extends JPanel {
    private final ShootingGameMain MAIN;
    private final AnimationManager AM;
    private final ResourceManager RM;

    private Map<ResourceKinds, String> animationId = new HashMap<>();

    private Vector2 panelSize; // 報酬リストパネルの大きさ
    private Vector2 originPos; // ドラッグ開始時のパネルの位置
    private double scrollOffset = 0; // スクロール量
    private double maxScrollOffset = 0; // 最大スクロール量
    private int col = 4; // 一行の個数

    public RewardListPanel() {
        MAIN = ShootingGameMain.getInstance();
        AM = AnimationManager.getInstance();
        RM = ResourceManager.getInstance();
    }

    public void initComponents() {
        panelSize = new Vector2(
                380 * ShootingGameMain.getScreenSizeAdj(),
                MAIN.getHeight() / 2);
        setLayout(null);
        setOpaque(false);
        setBounds(MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        // スクロールは負の値(上にずらす)。ホイールとドラッグの両方で動かせる
        addMouseWheelListener(e -> {
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * 30, -maxScrollOffset, 0);
            repaint();
        });
        java.awt.event.MouseAdapter drag = new java.awt.event.MouseAdapter() {
            private int lastY;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                lastY = e.getY();
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                scrollOffset = Math.clamp(scrollOffset + (e.getY() - lastY), -maxScrollOffset, 0);
                lastY = e.getY();
                repaint();
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    /**
     * 初期化
     */
    public void setup() {
        StageRewards rewards = StageRewards.getInstance();

        // 難易度・ミッションの行(GameResultSceneUIが描く)にかぶらないよう、パネルの上端を下げる(下端は変えない)
        double adj = ShootingGameMain.getScreenSizeAdj();
        int textLines = (rewards.getDifficultyLabel().isEmpty() ? 0 : 1) + rewards.getNewMissions().size();
        int bottom = MAIN.getHeight() / 2 + MAIN.getHeight() / 4;
        int top = Math.max(MAIN.getHeight() / 4, (int) (172 * adj + 15 * adj * textLines + 5 * adj));
        panelSize = new Vector2(panelSize.getX(), Math.max(60 * adj, bottom - top));
        setBounds(getX(), top, (int) panelSize.getX(), (int) panelSize.getY());

        // 最後の行が端数でも数える(切り上げ)
        int rows = (rewards.getResources().size() + col - 1) / col;
        double moduleH = moduleBlockHeight();
        // スクロール制限(全行の高さ - パネルの高さ。名前と個数の文字の分も足す)
        maxScrollOffset = Math.max(0,
                20 * ShootingGameMain.getScreenSizeAdj() + moduleH +
                        rows * (100 * ShootingGameMain.getScreenSizeAdj() + 10 * ShootingGameMain.getScreenSizeAdj())
                        + 30 * ShootingGameMain.getScreenSizeAdj()
                        - panelSize.getY());
        scrollOffset = 0;

        for (ResourceKinds resKind : rewards.getResources().keySet()) {
            BufferedImage[] imageFrames = null;
            boolean useView = false;
            // 画像情報の取得(表示用画像があればそちらを優先)
            ResourceName resName = resKind.getResName();
            Map<Rarity, BufferedImage[]> viewMap = RM.getResourceView().get(resName);
            if (viewMap != null && viewMap.get(resKind.getRarity()) != null) {
                imageFrames = viewMap.get(resKind.getRarity());
                useView = true;
            } else {
                Map<Rarity, BufferedImage[]> rarityMap = RM.getResourceImage().get(resName);
                if (rarityMap != null)
                    imageFrames = rarityMap.get(resKind.getRarity());
            }

            ResourceData resData = RM.getResourceConfig()
                    .get(resKind.getResName())
                    .getResData()
                    .get(resKind.getRarity());

            if (resData != null) {
                double frameRate = useView && resData.getViewFrameRate() > 0
                        ? resData.getViewFrameRate()
                        : resData.getFrameRate();
                double imageSize = 100 * ShootingGameMain.getScreenSizeAdj(); // 描画時に再調整入るためそれより大きめに設定
                animationId.put(resKind, AM.addAnimation(new Animation(imageFrames, imageSize, frameRate)));
            }
        }
    }

    /**
     * モジュールの表示に使う高さ
     */
    private double moduleBlockHeight() {
        int n = StageRewards.getInstance().getModules().size();
        return n == 0 ? 0 : (n * 20 + 8) * ShootingGameMain.getScreenSizeAdj();
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void drawPanel(Graphics g) {
        g.setClip(getX(), getY(), getWidth(), getHeight());

        StageRewards rewards = StageRewards.getInstance();
        double adj = ShootingGameMain.getScreenSizeAdj();

        // 手に入ったモジュール(素材の一覧の上に、1つ1行で出す)
        if (!rewards.getModules().isEmpty()) {
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, Math.max(10, (int) (12 * adj))));
            double lineH = 20 * adj;
            for (int i = 0; i < rewards.getModules().size(); i++) {
                Module m = rewards.getModules().get(i);
                g.setColor(CostPanel.rarityColor(m.getRarity()).brighter());
                g.drawString("★ " + ModuleService.title(m) + "  "
                        + m.getKind().getMainStat().format(ModuleService.mainNow(m)),
                        getX() + (int) (10 * adj), getY() + (int) (10 * adj + scrollOffset + lineH * (i + 1) - 4 * adj));
            }
        }

        // 基準点
        originPos = new Vector2(
                10 * adj,
                10 * adj + scrollOffset + moduleBlockHeight());

        int index = 0;
        LinkedHashMap<ResourceKinds, Integer> sortedRewards = UIBuilder.sortItemList(rewards.getResources());
        for (Map.Entry<ResourceKinds, Integer> entry : sortedRewards.entrySet()) {
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
                    originPos.getX() + size.getX() / 2 + (size.getX() + spacing) * posInd.getX(),
                    originPos.getY() + size.getY() / 2 + (size.getY() + spacing) * posInd.getY());
            UIBuilder.drawEquipmentsIcon(this, g, pos, size, animationId.get(name), name, amount);

            index++;
        }

        g.setClip(null);
    }
}
