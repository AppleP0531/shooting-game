package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.upgradeScene.SkillTreeLayout;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.upgradeScene.UpgradeManager.SkillTreeData;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class SkillTreeBtnPanel extends JPanel {
    private static final String component = "[SkillTreeBtnPanel]";
    private final ShootingGameMain MAIN;
    private final UpgradeManager UM;
    @Getter
    private final SkillDetailPanel skillDetailPanel;

    private MouseAdapter mouseHandler;
    private Vector2 dragStartPos; // ドラッグ開始マウス位置
    private Vector2 originPos; // ドラッグ開始時の基準点の位置
    private Vector2 dragMousePos; // ドラッグ中のマウス位置
    private double scale = 1; // ズーム量
    private Vector2 panelSize; // パネルサイズ
    private Vector2 skillTreeBtnOrigin; // 基準点の位置

    private Map<String, JButton> buttonMap;
    private Map<String, double[]> layoutCache; // 機体ごとの模様の配置
    private String layoutShip; // layoutCache を作ったときの機体

    public SkillTreeBtnPanel() {
        MAIN = ShootingGameMain.getInstance();
        UM = UpgradeManager.getInstance();
        skillDetailPanel = new SkillDetailPanel();

        mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPos = new Vector2(e.getX(), e.getY());
                originPos = new Vector2(skillTreeBtnOrigin.getX(), skillTreeBtnOrigin.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPos = null;
                dragMousePos = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragMousePos = new Vector2(e.getX(), e.getY());
                skillTreeBtnOrigin.setX(originPos.getX() + dragMousePos.getX() - dragStartPos.getX());
                skillTreeBtnOrigin.setY(originPos.getY() + dragMousePos.getY() - dragStartPos.getY());
                drawPanel();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomAmount = 0.1; // ズーム変化量
                if (e.getPreciseWheelRotation() < 0) {
                    scale = Math.min(scale + zoomAmount, 2);
                } else {
                    scale = Math.max(scale - zoomAmount, 0.4);
                }

                reload();
            }
        };
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        // 画面下のボタン列(CONVERT / UNIT / MODULE)に重ならないよう、その上までをスキルツリーの領域にする
        double top = MAIN.getHeight() / 12.0;
        double bottom = MAIN.getHeight() - 116 * ShootingGameMain.getScreenSizeAdj();
        panelSize = new Vector2(MAIN.getWidth(), bottom - top);
        setLayout(null);
        setOpaque(false);
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                (int) top,
                (int) panelSize.getX(), (int) panelSize.getY());

        skillDetailPanel.initComponents();
    }

    /**
     * セットアップ
     */
    public void setup() {
        skillTreeBtnOrigin = new Vector2(panelSize.getX() / 2, panelSize.getY() / 2);

        // 最初は、ツリー全体が画面に収まる大きさにする(ホイールで拡大・縮小できる)
        double adj = ShootingGameMain.getScreenSizeAdj();
        double[] ext = SkillTreeLayout.extent(currentLayout());
        double fitX = (panelSize.getX() / 2 - 28 * adj) / Math.max(1, ext[0] * adj);
        double fitY = (panelSize.getY() / 2 - 28 * adj) / Math.max(1, ext[1] * adj);
        scale = Math.max(0.45, Math.min(1.0, Math.min(fitX, fitY)));

        drawBtn();
        drawPanel();

        // 最前面に持ってくる
        if (getParent() != null)
            getParent().setComponentZOrder(this, 0);
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Vector2 panelPos = new Vector2(this.getX(), this.getY());

        g2.setClip(getX(), getY(), getWidth(), getHeight());
        g2.setColor(Color.CYAN);
        g2.setStroke(new BasicStroke(2));

        for (SkillTreeData data : UM.getSkillTreeConfigMap()) {
            if (data.getName().equals("Origin"))
                continue;
            JButton parent = buttonMap.get(data.getRootNode());
            JButton child = buttonMap.get(data.getName());
            // 画像キャッシュにないスキルはボタンが作られないので飛ばす
            if (parent == null || child == null)
                continue;

            // 親ボタンの中心座標
            Vector2 parentPos = new Vector2(
                    panelPos.getX() + parent.getX() + parent.getWidth() / 2,
                    panelPos.getY() + parent.getY() + parent.getHeight() / 2);
            // 子ボタンの中心座標
            Vector2 childPos = new Vector2(
                    panelPos.getX() + child.getX() + child.getWidth() / 2,
                    panelPos.getY() + child.getY() + child.getHeight() / 2);

            if (data.getName().equals("Origin") || SkillService.isUnlocked(data.getName())) {
                // パネルのY座標の範囲
                int panelTop = (int) panelPos.getY();
                int panelBottom = panelTop + getHeight();

                // 親と子がパネルの表示領域に入っているかの判定
                boolean parentInView = (parentPos.getY() >= panelTop && parentPos.getY() <= panelBottom);
                boolean childInView = (childPos.getY() >= panelTop && childPos.getY() <= panelBottom);

                // どちらかが入っている場合に線で結ぶ
                if (parentInView || childInView)
                    g2.drawLine((int) parentPos.getX(), (int) parentPos.getY(),
                            (int) childPos.getX(), (int) childPos.getY());
            }
        }

        // 研究中なら、パネル上部に研究中のスキルと残り時間を出す
        String researching = SkillService.researchingSkill();
        if (researching != null) {
            SkillTreeData rd = SkillService.find(researching);
            String text = "研究中: " + (rd != null ? rd.getTitle() : researching)
                    + "  残り " + SkillService.formatTime(SkillService.researchRemaining());
            g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD,
                    (float) (13 * ShootingGameMain.getScreenSizeAdj())));
            g2.setColor(new Color(255, 214, 80));
            g2.drawString(text, getX() + 8, getY() + (int) (18 * ShootingGameMain.getScreenSizeAdj()));
        }

        g2.setClip(null);
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        drawBtn();
        drawPanel();
    }

    /**
     * いまの機体の模様での、各ボタンの位置(基準点からのずれ)。機体が変わったら作り直す
     */
    private Map<String, double[]> currentLayout() {
        String ship = DataManager.getUserData() == null ? null : DataManager.getUserData().getSelectedShip();
        if (layoutCache == null || !java.util.Objects.equals(ship, layoutShip)) {
            List<SkillTreeLayout.Node> nodes = new ArrayList<>();
            for (SkillTreeData d : UM.getSkillTreeConfigMap())
                nodes.add(new SkillTreeLayout.Node(d.getName(), d.getRootNode(), d.getPos()));
            layoutCache = SkillTreeLayout.compute(SkillTreeLayout.patternFor(ship), nodes);
            layoutShip = ship;
        }
        return layoutCache;
    }

    /**
     * パネルの描画
     */
    private void drawPanel() {
        // パネルサイズが正しく取得できていなかった場合処理をスキップ
        if (panelSize.getX() == 0)
            return;

        setVisible(true);

        // スキルツリーボタンの基準点の設定(画面中央)
        if (skillTreeBtnOrigin == null)
            skillTreeBtnOrigin = new Vector2(this.getWidth() / 2, this.getHeight() / 2);

        // 機体ごとの模様で、各ボタンの位置を決める
        Map<String, double[]> layout = currentLayout();

        // スキルツリーボタンの追加
        for (SkillTreeData data : UM.getSkillTreeConfigMap()) {
            JButton button = buttonMap.get(data.getName());
            if (button == null)
                continue;

            button.setVisible(false);

            // 各ボタンの位置設定(機体ごとの模様。基準点からのずれに画面倍率とズームをかける)
            double[] off = layout.get(data.getName());
            if (off == null)
                continue;
            double k = scale * ShootingGameMain.getScreenSizeAdj();
            Vector2 buttonPos = new Vector2(
                    skillTreeBtnOrigin.getX() + off[0] * k - button.getWidth() / 2,
                    skillTreeBtnOrigin.getY() + off[1] * k - button.getHeight() / 2);
            // 位置
            int x = (int) buttonPos.getX();
            int y = (int) buttonPos.getY();
            button.setBounds(
                    x, y,
                    (int) button.getWidth(), (int) button.getHeight());

            boolean isSelfInPanel = (0 < y + button.getHeight() && y < this.getHeight());
            boolean isParentInPanel = false;
            JButton parentBtn = buttonMap.get(data.getRootNode());
            if (parentBtn != null) // 親が存在する場合
                isParentInPanel = (0 < y + parentBtn.getHeight() && y < this.getHeight());
            // どちらかがパネル内にある場合
            if (isSelfInPanel || isParentInPanel) {
                // 基準ボタンではない時
                if (!data.getName().equals("Origin")) {
                    // 親が未強化(解放条件未達)なら非表示のままにする
                    if (!SkillService.isUnlocked(data.getName()))
                        continue;
                }

                button.setVisible(true);
            }
        }
    }

    /**
     * ボタンの描画
     */
    private void drawBtn() {
        this.removeAll();

        buttonMap = new HashMap<>();

        for (Map.Entry<String, BufferedImage> entry : UM.getSkillTreeImageCache().entrySet()) {
            String skillName = entry.getKey();
            BufferedImage glyph = entry.getValue();
            SkillTreeData sd0 = SkillService.find(skillName);
            int maxLv = sd0 == null ? 1 : sd0.getMaxLevel();
            // 模様の画像があれば、枠とレベルの札を重ねたボタン画像にする(最大レベルは金の枠)
            BufferedImage image = glyph == null ? null
                    : SkillIconComposer.compose(glyph, SkillService.getLevel(skillName), maxLv, "Origin".equals(skillName));

            Vector2 skillTreeBtnSize = new Vector2(
                    44 * scale * ShootingGameMain.getScreenSizeAdj(),
                    44 * scale * ShootingGameMain.getScreenSizeAdj());
            JButton button = UIBuilder.createImageBtn(
                    component, image, skillTreeBtnSize, skillName);

            button.addActionListener(e -> {
                skillDetailPanel.showSkillDetail(skillName);
            });
            button.setRolloverEnabled(false);

            this.add(button);
            button.setVisible(false);

            // ボタン画像のセット、画像がなければテキストをセット
            if (button.getIcon() != null) {
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setText("");
                SkillTreeData tip = SkillService.find(skillName);
                button.setToolTipText((tip != null && tip.getTitle() != null ? tip.getTitle() : skillName)
                        + (maxLv > 1 ? "  Lv" + SkillService.getLevel(skillName) + "/" + maxLv : ""));
            } else {
                // 画像がないときは短い表示名と現在のレベルを表示
                SkillTreeData sd = SkillService.find(skillName);
                String label = sd != null && sd.getTitle() != null ? sd.getTitle() : skillName;
                int lv = SkillService.getLevel(skillName);
                button.setText(lv > 0 ? "<html><center>" + label + "<br>Lv" + lv + "</center></html>" : label);
                button.setFont(button.getFont().deriveFont((float) (10 * scale * ShootingGameMain.getScreenSizeAdj())));
                button.setMargin(new java.awt.Insets(0, 0, 0, 0));
            }

            buttonMap.put(skillName, button);
        }

        this.add(skillDetailPanel);
        skillDetailPanel.setVisible(false);
    }

    /**
     * ハンドラの追加
     */
    public void addHandler() {
        MAIN.addMouseListener(mouseHandler);
        MAIN.addMouseMotionListener(mouseHandler);
        MAIN.addMouseWheelListener(mouseHandler);
    }

    /**
     * ハンドラの削除
     */
    public void removeHandler() {
        MAIN.removeMouseListener(mouseHandler);
        MAIN.removeMouseMotionListener(mouseHandler);
        MAIN.removeMouseWheelListener(mouseHandler);
    }
}
