package com.game.shooting_game.upgradeScene.subPanels;

import com.game.shooting_game.ui.UIStyle;
import static com.game.shooting_game.ui.UIStyle.px;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.domain.subship.SubShipTypeService;
import com.game.shooting_game.domain.subship.SubShipTypeService.TypeDef;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * サブ機工房パネル
 * タイプごとにコインとパーツ素材でレベルを上げ、レベルが上がると撃つ弾の数と種類が増える。
 * 画面(縦長 405x720 が基準)に収まる大きさで、上から「状態 → タイプ → 説明 → プレビュー → コスト → ボタン」の順に並べる
 */
public class SubShipPanel extends JPanel {
    private static final String component = "[SubShipPanel]";

    private final ShootingGameMain MAIN;
    private Vector2 panelSize;

    private JTextArea summaryArea;
    private JTextArea detailArea;
    private JTextArea statusArea;
    private CostPanel costPanel;
    private SubShipPreview preview;
    private JLabel typeLabel;
    private JLabel typeDetailLabel; // 見ているタイプの説明(1〜2行)
    private TypeDots typeDots; // 何番目のタイプかを示す ◇ の並び
    private JButton typePrevBtn;
    private JButton typeNextBtn;
    private JButton typeActionBtn; // 解放する / これにする
    private JButton upgradeBtn;
    private JButton equipBtn;
    private TypeDef viewingType; // 見ているタイプ(使用中とは限らない。強化の対象)

    public SubShipPanel() {
        MAIN = ShootingGameMain.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        // 基準サイズは画面幅(405)より少し小さい幅にする
        panelSize = new Vector2(px(385), px(556));
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        int pad = px(8);
        int innerW = (int) panelSize.getX() - 2 * pad;

        // 全体の状態
        summaryArea = UIStyle.textArea(12, false);
        summaryArea.setBounds(pad, px(6), innerW, px(34));
        this.add(summaryArea);

        // タイプの切り替え(◀ ▶ で見て、解放済みなら選べる。未解放はコインで解放する。強化は見ているタイプに対して行う)
        int typeY = px(46);
        typePrevBtn = new JButton("◀");
        typeNextBtn = new JButton("▶");
        typeLabel = new JLabel("", SwingConstants.CENTER);
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(13))));
        typePrevBtn.setBounds(pad, typeY, px(40), px(26));
        typeNextBtn.setBounds(pad + innerW - px(40), typeY, px(40), px(26));
        int actionW = px(100);
        typeActionBtn = new JButton();
        typeActionBtn.setBounds(pad + innerW - px(44) - actionW, typeY, actionW, px(26));
        typeLabel.setBounds(pad + px(44), typeY, innerW - px(88) - actionW - px(4), px(26));
        for (JButton b : new JButton[] { typePrevBtn, typeNextBtn, typeActionBtn }) {
            b.setMargin(new java.awt.Insets(0, 0, 0, 0));
            b.setFocusPainted(false);
            this.add(b);
        }
        typePrevBtn.addActionListener(e -> {
            viewingType = SubShipTypeService.neighbor(viewingType, -1);
            refresh();
        });
        typeNextBtn.addActionListener(e -> {
            viewingType = SubShipTypeService.neighbor(viewingType, 1);
            refresh();
        });
        typeActionBtn.addActionListener(e -> {
            if (!SubShipTypeService.isUnlocked(viewingType))
                SubShipTypeService.unlock(viewingType);
            else
                SubShipTypeService.select(viewingType);
            refresh();
        });
        this.add(typeLabel);

        // タイプの並び(◇)。見ているタイプが塗りつぶされ、使用中は金色
        typeDots = new TypeDots();
        typeDots.setBounds(pad, px(76), innerW, px(14));
        this.add(typeDots);

        // タイプの説明(行の下に小さく。長くても2行に収める)
        typeDetailLabel = new JLabel("");
        typeDetailLabel.setForeground(new Color(150, 200, 230));
        typeDetailLabel.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        typeDetailLabel.setVerticalAlignment(SwingConstants.TOP);
        typeDetailLabel.setBounds(pad + px(4), px(92), innerW - px(8), px(32));
        this.add(typeDetailLabel);

        // プレビュー(いまのサブ機と、1レベル強化した後のサブ機)
        preview = new SubShipPreview();
        preview.setBounds(pad, px(128), innerW, px(110));
        this.add(preview);

        // 説明(いまのレベルの撃ち方と、次のレベルで変わること)
        detailArea = UIStyle.textArea(12, false);
        detailArea.setBounds(pad, px(244), innerW, px(84));
        this.add(detailArea);

        // コスト(アイコン + 所有pt/必要pt + 持っている素材のレア度)。最大でコイン+素材6種=4行
        costPanel = new CostPanel(2);
        costPanel.setBounds(pad, px(332), innerW, costPanel.maxHeight());
        this.add(costPanel);

        // 状態メッセージ
        statusArea = UIStyle.textArea(12, false);
        statusArea.setBounds(pad, px(332) + costPanel.maxHeight() + px(2), innerW, px(34));
        this.add(statusArea);

        // 強化ボタン・装備ボタン
        Vector2 btnSize = new Vector2(px(150), px(40));
        int btnY = (int) panelSize.getY() - (int) btnSize.getY() - px(8);
        upgradeBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/craftBtn.png", btnSize, "UPGRADE");
        upgradeBtn.setBounds((int) panelSize.getX() / 2 - (int) btnSize.getX() - px(6), btnY,
                (int) btnSize.getX(), (int) btnSize.getY());
        upgradeBtn.addActionListener(e -> {
            if (SubShipService.upgrade(viewingType))
                refresh();
        });
        this.add(upgradeBtn);

        equipBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/equipBtn.png", btnSize, "EQUIP");
        equipBtn.setBounds((int) panelSize.getX() / 2 + px(6), btnY,
                (int) btnSize.getX(), (int) btnSize.getY());
        equipBtn.addActionListener(e -> {
            SubShipService.setEquipped(!SubShipService.isEquipped());
            refresh();
        });
        this.add(equipBtn);
    }

    /**
     * 表示内容の更新
     */
    private void refresh() {
        if (viewingType == null)
            viewingType = SubShipTypeService.current();

        // タイプ(見ているタイプの名前・状態と、解放/選択のボタン)
        boolean own = SubShipTypeService.isUnlocked(viewingType);
        boolean inUse = viewingType.equals(SubShipTypeService.current());
        typeLabel.setText("" + (SubShipTypeService.types().indexOf(viewingType) + 1) + "/" + SubShipTypeService.types().size() + "  " + viewingType.title() + (own ? "  Lv" + SubShipService.level(viewingType) : ""));
        typeDetailLabel.setText("<html>" + viewingType.detail() + "</html>");
        typeActionBtn.setText(!own ? "解放 " + String.format("%,d", viewingType.unlockCoin()) : inUse ? "使用中" : "これにする");
        typeActionBtn.setEnabled(!own ? SubShipTypeService.canUnlock(viewingType) : !inUse);

        typeDots.setViewing(viewingType);
        preview.setType(own ? viewingType : null);
        summaryArea.setText(SubShipService.summary());
        detailArea.setText(own ? SubShipService.describe(viewingType) : "解放すると強化できます");
        detailArea.setCaretPosition(0);

        Cost cost = own ? SubShipService.getCost(viewingType) : null;
        costPanel.setCost(cost);
        statusArea.setText(SubShipService.status(viewingType));

        upgradeBtn.setEnabled(own && SubShipService.canAfford(cost));
        equipBtn.setEnabled(true);
        // 画像がないときはボタンの文字を状態に合わせる
        if (equipBtn.getIcon() == null)
            equipBtn.setText(SubShipService.isEquipped() ? "取り外す" : "装備する");
        if (upgradeBtn.getIcon() == null)
            upgradeBtn.setText("強化する");
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        viewingType = SubShipTypeService.current();
        refresh();

        UIBuilder.openPanel(this, "サブ機工房", true, null);
        // 後ろのスキルツリーが透けて文字が読みにくくならないよう、背景を濃くする
        setBackground(UIStyle.PANEL_BG);
    }
}
