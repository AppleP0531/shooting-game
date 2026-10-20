package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.event.MouseEvent;
import javax.swing.SwingConstants;
import com.game.shooting_game.ui.ModuleIcons;
import com.game.shooting_game.ui.UIStyle;
import static com.game.shooting_game.ui.UIStyle.px;
import com.game.shooting_game.domain.cost.CostService;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.module.Module;
import com.game.shooting_game.domain.module.ModuleInventory;
import com.game.shooting_game.domain.module.ModuleKind;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;

/**
 * モジュールパネル
 * 上から 装着中の4枠 → 持っているモジュール → 選んだモジュールの能力 → 新しく作る(種類・レア度・必要素材)。
 */
public class ModulePanel extends JPanel {
    private final JButton[] slotBtns = new JButton[ModuleInventory.SLOTS];
    private final DefaultListModel<Module> listModel = new DefaultListModel<>();
    private JList<Module> list;
    private JTextArea detailArea;
    private JButton equipBtn;
    private JButton levelUpBtn;
    private JButton dismantleBtn;
    private JComboBox<ModuleKind> kindBox;
    private JComboBox<Rarity> rarityBox;
    private CostPanel costPanel;
    private JButton craftBtn;
    private JTextArea statusArea;
    private JLabel lastLabel;

    private JTextArea area(double size) {
        return UIStyle.textArea(size, false);
    }

    private JButton button(String text, int x, int y, int w, int h) {
        JButton b = UIStyle.button(text, 11);
        b.setBounds(x, y, w, h);
        add(b);
        return b;
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        int w = px(385);
        int h = px(574);
        setBounds(ShootingGameMain.getInstance().getWidth() / 2 - w / 2,
                ShootingGameMain.getInstance().getHeight() / 2 - h / 2, w, h);
        int pad = px(8);
        int innerW = w - 2 * pad;

        // 装着中の4枠(押すと外す)
        int gap = px(4);
        int sw = (innerW - gap * (ModuleInventory.SLOTS - 1)) / ModuleInventory.SLOTS;
        for (int i = 0; i < ModuleInventory.SLOTS; i++) {
            slotBtns[i] = button("", pad + i * (sw + gap), px(6), sw, px(56));
            final int idx = i;
            slotBtns[i].addActionListener(e -> {
                Module m = ModuleService.slotted()[idx];
                if (m != null) {
                    ModuleService.unequip(m.getId());
                    refresh();
                }
            });
        }

        // 持っているモジュール
        // アイコンだけを並べる(レベルと装備中は絵の上に出す。名前と値はマウスを乗せると出る)
        list = new JList<>(listModel) {
            @Override
            public String getToolTipText(MouseEvent ev) {
                int i = locationToIndex(ev.getPoint());
                if (i < 0 || !getCellBounds(i, i).contains(ev.getPoint()))
                    return null;
                Module m = getModel().getElementAt(i);
                return ModuleService.title(m) + "  " + m.getKind().getMainStat().format(ModuleService.mainNow(m));
            }
        };
        list.setToolTipText("");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UIStyle.darkList(list);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(px(52));
        list.setFixedCellHeight(px(52));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel,
                    boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, sel, focus);
                Module m = (Module) value;
                c.setIcon(ModuleIcons.icon(m, px(44), ModuleService.isEquipped(m)));
                c.setText("");
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setBackground(sel ? UIStyle.LIST_SELECTED : UIStyle.LIST_BG);
                return c;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                refreshDetail();
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBounds(pad, px(68), innerW, px(124));
        add(scroll);

        // 選んだモジュールの能力
        detailArea = area(11);
        detailArea.setBounds(pad, px(196), innerW, px(106));
        add(detailArea);

        int bw = (innerW - 2 * px(4)) / 3;
        int by = px(304);
        equipBtn = button("装着", pad, by, bw, px(30));
        levelUpBtn = button("レベルアップ", pad + bw + px(4), by, bw, px(30));
        dismantleBtn = button("分解(基板が戻る)", pad + 2 * (bw + px(4)), by, bw, px(30));
        equipBtn.addActionListener(e -> {
            Module m = list.getSelectedValue();
            if (m == null)
                return;
            if (ModuleService.isEquipped(m))
                ModuleService.unequip(m.getId());
            else if (!ModuleService.equip(m.getId())) {
                statusArea.setText("空き枠がありません。装着中のモジュールを外してください");
                return;
            }
            refresh();
        });
        levelUpBtn.addActionListener(e -> {
            Module m = list.getSelectedValue();
            if (m == null)
                return;
            int before = m.getSubs().size();
            if (ModuleService.levelUp(m.getId())) {
                refresh();
                statusArea.setText(m.getSubs().size() > before
                        ? "Lv" + m.getLevel() + " になり、サブ能力が増えました"
                        : "Lv" + m.getLevel() + " になりました");
            }
        });
        dismantleBtn.addActionListener(e -> {
            Module m = list.getSelectedValue();
            if (m != null && ModuleService.dismantle(m.getId()))
                refresh();
        });

        // 制作
        JLabel craftLabel = new JLabel("新しく作る(サブ能力はレベルが上がると増える)");
        craftLabel.setForeground(new Color(200, 230, 255));
        craftLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
        craftLabel.setBounds(pad, px(340), innerW, px(16));
        add(craftLabel);

        kindBox = new JComboBox<>(ModuleKind.values());
        rarityBox = new JComboBox<>(Rarity.values());
        kindBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel,
                    boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, sel, focus);
                ModuleKind k = (ModuleKind) value;
                c.setText(k.getId() + "  " + k.getTitle());
                return c;
            }
        });
        rarityBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel,
                    boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, sel, focus);
                c.setText(ModuleService.rarityName((Rarity) value));
                c.setForeground(CostPanel.rarityColor((Rarity) value).darker());
                return c;
            }
        });
        kindBox.setBounds(pad, px(358), innerW * 6 / 10 - px(2), px(26));
        rarityBox.setBounds(pad + innerW * 6 / 10 + px(2), px(358), innerW * 4 / 10 - px(2), px(26));
        kindBox.addActionListener(e -> refreshCraft());
        rarityBox.addActionListener(e -> refreshCraft());
        add(kindBox);
        add(rarityBox);

        lastLabel = new JLabel("");
        lastLabel.setForeground(Color.WHITE);
        lastLabel.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
        lastLabel.setBounds(pad, px(386), innerW, px(16));
        add(lastLabel);

        costPanel = new CostPanel(1);
        costPanel.setBounds(pad, px(404), innerW, costPanel.maxHeight());
        add(costPanel);

        craftBtn = button("制作する", pad, px(404) + costPanel.maxHeight() + px(4), innerW, px(34));
        craftBtn.addActionListener(e -> {
            Module m = ModuleService.craft((ModuleKind) kindBox.getSelectedItem(),
                    (Rarity) rarityBox.getSelectedItem());
            if (m != null) {
                refresh();
                list.setSelectedValue(m, true);
                statusArea.setText("できました: " + ModuleService.title(m));
            }
        });

        statusArea = area(11);
        statusArea.setBounds(pad, px(404) + costPanel.maxHeight() + px(40), innerW, px(30));
        add(statusArea);
    }

    private void refreshDetail() {
        Module m = list.getSelectedValue();
        if (m == null) {
            detailArea.setText("持っているモジュールを選ぶと能力が見られます");
            equipBtn.setEnabled(false);
            levelUpBtn.setEnabled(false);
            dismantleBtn.setEnabled(false);
            return;
        }
        var upCost = ModuleService.levelUpCost(m);
        detailArea.setText(ModuleService.describe(m)
                + (upCost == null ? "【最大レベル】" : "Lv" + (m.getLevel() + 1) + "まで: コイン " + upCost.coins()
                        + " / 基板×" + upCost.items().values().iterator().next()));
        levelUpBtn.setEnabled(upCost != null && CostService.canAfford(upCost));
        equipBtn.setEnabled(true);
        equipBtn.setText(ModuleService.isEquipped(m) ? "外す" : "装着");
        dismantleBtn.setEnabled(!ModuleService.isEquipped(m));
        dismantleBtn.setText("分解(基板×" + ModuleService.dismantleRefund(m.getRarity()) + ")");
    }

    private void refreshCraft() {
        ModuleKind k = (ModuleKind) kindBox.getSelectedItem();
        Rarity r = (Rarity) rarityBox.getSelectedItem();
        if (k == null || r == null)
            return;
        var cost = ModuleService.recipe(k, r);
        costPanel.setCost(cost);
        boolean ok = CostService.canAfford(cost);
        craftBtn.setEnabled(ok);
        lastLabel.setText(k.getDetail() + "  メイン " + k.getMainStat().format(ModuleService.mainValue(k, r)));
        statusArea.setText(ok ? "制作できます" : "コインか素材が足りません(素材は変換所でレア度を上げられます)");
    }

    /**
     * 表示内容の更新
     */
    private void refresh() {
        Module[] slotted = ModuleService.slotted();
        for (int i = 0; i < slotBtns.length; i++) {
            Module m = slotted[i];
            // 絵を上、能力の値を下に出す
            slotBtns[i].setVerticalTextPosition(SwingConstants.BOTTOM);
            slotBtns[i].setHorizontalTextPosition(SwingConstants.CENTER);
            if (m == null) {
                slotBtns[i].setIcon(null);
                slotBtns[i].setText("空き");
                slotBtns[i].setToolTipText(null);
                slotBtns[i].setForeground(new Color(120, 120, 120));
            } else {
                slotBtns[i].setIcon(ModuleIcons.icon(m, px(36)));
                slotBtns[i].setText(m.getKind().getMainStat().format(ModuleService.mainNow(m)));
                slotBtns[i].setToolTipText(ModuleService.title(m) + "(押すと外す)");
                slotBtns[i].setForeground(CostPanel.rarityColor(m.getRarity()).darker());
            }
        }

        long selId = list.getSelectedValue() == null ? 0 : list.getSelectedValue().getId();
        List<Module> sorted = new ArrayList<>(ModuleService.owned());
        sorted.sort(Comparator.comparing((Module m) -> m.getRarity()).reversed()
                .thenComparing(m -> m.getKind()).thenComparing(Module::getId));
        listModel.clear();
        for (Module m : sorted) {
            listModel.addElement(m);
            if (m.getId() == selId)
                list.setSelectedValue(m, false);
        }
        refreshDetail();
        refreshCraft();
        repaint();
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        refresh();
        UIBuilder.openPanel(this, "モジュール", true, null);
        setBackground(UIStyle.PANEL_BG);
    }
}
