package com.game.shooting_game.upgradeScene.subPanels;

import static com.game.shooting_game.ui.UIStyle.px;

import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.armor.ArmorService;
import com.game.shooting_game.domain.armor.ArmorService.Type;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.ui.UIStyle;

/**
 * 装甲の強化パネル(防御装甲・火力装甲)
 * レベル(1〜20)をコインと強化素材で上げる。★はレベル4ごとに1つ。効果はいまの値と、次のレベルの値を並べて出す。
 */
public class ArmorPanel extends JPanel {
    private final Type type;
    private JTextArea headerArea;
    private JTextArea effectArea;
    private CostPanel costPanel;
    private JButton levelBtn;
    private JTextArea statusArea;

    public ArmorPanel(Type type) {
        this.type = type;
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        int w = px(385);
        int h = px(330);
        setBounds(ShootingGameMain.getInstance().getWidth() / 2 - w / 2,
                ShootingGameMain.getInstance().getHeight() / 2 - h / 2, w, h);
        int pad = px(8);
        int innerW = w - 2 * pad;

        headerArea = UIStyle.textArea(15, true);
        headerArea.setBounds(pad, px(8), innerW, px(64));
        add(headerArea);

        effectArea = UIStyle.textArea(12, false);
        effectArea.setBounds(pad, px(76), innerW, px(84));
        add(effectArea);

        costPanel = new CostPanel(1);
        costPanel.setBounds(pad, px(164), innerW, costPanel.maxHeight());
        add(costPanel);

        int y = px(164) + costPanel.maxHeight() + px(4);
        levelBtn = UIStyle.button("レベルアップ", 12);
        levelBtn.setBounds(pad, y, innerW, px(36));
        levelBtn.addActionListener(e -> {
            if (ArmorService.levelUp(type))
                refresh();
        });
        add(levelBtn);
        y += px(44);

        statusArea = UIStyle.textArea(11, false);
        statusArea.setBounds(pad, y, innerW, px(34));
        add(statusArea);
    }

    /** ★の文字(最大まで並べ、上げた分を塗りつぶす) */
    static String starText(int stars) {
        return "★".repeat(stars) + "☆".repeat(ArmorService.MAX_STARS - stars);
    }

    /**
     * 表示内容の更新
     */
    private void refresh() {
        int lv = ArmorService.level(type);
        boolean max = lv >= ArmorService.MAX_LEVEL;
        headerArea.setText(type.title() + "\nLv " + lv + " / " + ArmorService.MAX_LEVEL + "   "
                + starText(ArmorService.stars(lv)) + "\n" + type.detail());

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ModuleStat, Double> e : type.perLevel().entrySet()) {
            ModuleStat s = e.getKey();
            sb.append(s.getLabel()).append("  ").append(s.format(ArmorService.value(type, s, lv)));
            if (!max)
                sb.append(" → ").append(s.format(ArmorService.value(type, s, lv + 1)));
            sb.append("\n");
        }
        sb.append("(モジュールと合わせて効きます)");
        effectArea.setText(sb.toString());

        costPanel.setCost(max ? null : ArmorService.levelCost(lv));
        levelBtn.setText(max ? "レベル最大" : "レベルアップ  Lv" + lv + " → Lv" + (lv + 1));
        boolean can = !max && CostService.canAfford(ArmorService.levelCost(lv));
        levelBtn.setEnabled(can);
        statusArea.setText(max ? "最大まで強化されています"
                : can ? "レベルアップができます" : "レベルアップにはコインか強化素材が足りません");
        repaint();
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        refresh();
        UIBuilder.openPanel(this, type.title(), true, null);
        setBackground(UIStyle.PANEL_BG);
    }
}
