package com.game.shooting_game.upgradeScene.subPanels;

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
import com.game.shooting_game.module.Module;
import com.game.shooting_game.module.ModuleInventory;
import com.game.shooting_game.module.ModuleKind;
import com.game.shooting_game.module.ModuleService;
import com.game.shooting_game.skill.SkillService;
import com.game.shooting_game.utils.CostPanel;
import com.game.shooting_game.utils.UIBuilder;

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
    private JButton dismantleBtn;
    private JComboBox<ModuleKind> kindBox;
    private JComboBox<Rarity> rarityBox;
    private CostPanel costPanel;
    private JButton craftBtn;
    private JTextArea statusArea;
    private JLabel lastLabel;

    private static int px(double base) {
        return (int) (base * ShootingGameMain.getScreenSizeAdj());
    }

    private JTextArea area(double size) {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);
        a.setForeground(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(size))));
        return a;
    }

    private JButton button(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, h);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
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
        int h = px(552);
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
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel,
                    boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, sel, focus);
                Module m = (Module) value;
                c.setText((ModuleService.isEquipped(m) ? "[装着] " : "") + ModuleService.title(m) + "  "
                        + m.getKind().getMainStat().format(m.getMainValue()));
                c.setForeground(CostPanel.rarityColor(m.getRarity()).brighter());
                c.setBackground(sel ? new Color(60, 90, 130) : new Color(20, 24, 34));
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
        detailArea.setBounds(pad, px(196), innerW, px(84));
        add(detailArea);

        equipBtn = button("装着", pad, px(282), innerW / 2 - px(3), px(30));
        dismantleBtn = button("分解(基板が戻る)", pad + innerW / 2 + px(3), px(282), innerW / 2 - px(3), px(30));
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
        dismantleBtn.addActionListener(e -> {
            Module m = list.getSelectedValue();
            if (m != null && ModuleService.dismantle(m.getId()))
                refresh();
        });

        // 制作
        JLabel craftLabel = new JLabel("新しく作る(サブ能力4つはランダム)");
        craftLabel.setForeground(new Color(200, 230, 255));
        craftLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
        craftLabel.setBounds(pad, px(318), innerW, px(16));
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
        kindBox.setBounds(pad, px(336), innerW * 6 / 10 - px(2), px(26));
        rarityBox.setBounds(pad + innerW * 6 / 10 + px(2), px(336), innerW * 4 / 10 - px(2), px(26));
        kindBox.addActionListener(e -> refreshCraft());
        rarityBox.addActionListener(e -> refreshCraft());
        add(kindBox);
        add(rarityBox);

        lastLabel = new JLabel("");
        lastLabel.setForeground(Color.WHITE);
        lastLabel.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
        lastLabel.setBounds(pad, px(364), innerW, px(16));
        add(lastLabel);

        costPanel = new CostPanel(1);
        costPanel.setBounds(pad, px(382), innerW, costPanel.maxHeight());
        add(costPanel);

        craftBtn = button("制作する", pad, px(382) + costPanel.maxHeight() + px(4), innerW, px(34));
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
        statusArea.setBounds(pad, px(382) + costPanel.maxHeight() + px(40), innerW, px(30));
        add(statusArea);
    }

    private void refreshDetail() {
        Module m = list.getSelectedValue();
        if (m == null) {
            detailArea.setText("持っているモジュールを選ぶと能力が見られます");
            equipBtn.setEnabled(false);
            dismantleBtn.setEnabled(false);
            return;
        }
        detailArea.setText(ModuleService.describe(m));
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
        boolean ok = SkillService.canAfford(cost);
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
            if (m == null) {
                slotBtns[i].setText("<html><center>空き<br>-</center></html>");
                slotBtns[i].setForeground(new Color(120, 120, 120));
            } else {
                slotBtns[i].setText("<html><center>" + m.getKind().getShortName() + " "
                        + ModuleService.rarityName(m.getRarity()) + "<br>"
                        + m.getKind().getMainStat().format(m.getMainValue()) + "</center></html>");
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
        setBackground(new Color(0, 0, 0, 245));
    }
}
