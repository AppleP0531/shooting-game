package com.game.shooting_game.stageScene;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.star.StarRewardService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * ★報酬パネル(★が一定数たまるごとの報酬を、ここで手動で受け取る)
 */
public class StarRewardPanel extends JPanel {
    private static final String component = "[StarRewardPanel]";
    private static final int UPCOMING = 3; // 未達成の表示数

    private final Runnable onChanged; // 受け取り後の通知(ボタン表記の更新用)
    private final JLabel summaryLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel listPanel = new JPanel();
    private final JButton claimAllBtn;
    private final double adj = ShootingGameMain.getScreenSizeAdj();

    public StarRewardPanel(Runnable onChanged) {
        this.onChanged = onChanged;
        setLayout(null);
        setVisible(false);

        ShootingGameMain main = ShootingGameMain.getInstance();
        int w = px(360);
        int h = (int) Math.min(480 * adj, main.getHeight() - 80 * adj);
        int sp = px(6);
        int titleBar = px(25);
        setBounds(main.getWidth() / 2 - w / 2, Math.max(titleBar + sp, main.getHeight() / 2 - h / 2), w, h);

        summaryLabel.setForeground(Color.YELLOW);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, (float) (16 * adj)));
        summaryLabel.setBounds(sp, sp, w - 2 * sp, px(24));
        add(summaryLabel);

        messageLabel.setForeground(Color.GREEN);
        messageLabel.setFont(messageLabel.getFont().deriveFont((float) (13 * adj)));
        messageLabel.setBounds(sp, sp + px(24), w - 2 * sp, px(20));
        add(messageLabel);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(px(16));
        int btnH = px(40);
        int listY = sp + px(48);
        scroll.setBounds(sp, listY, w - 2 * sp, h - listY - btnH - 2 * sp);
        add(scroll);

        claimAllBtn = textBtn("まとめて受け取る", px(160), btnH);
        claimAllBtn.setBounds(w / 2 - px(80), h - btnH - sp, px(160), btnH);
        claimAllBtn.addActionListener(e -> {
            Reward r = StarRewardService.claimAll();
            if (r != null)
                afterClaim("受け取りました: " + rewardText(r));
        });
        add(claimAllBtn);
    }

    /** パネルを開く */
    public void showPanel() {
        messageLabel.setText("");
        refresh();
        UIBuilder.openPanel(this, "★報酬", true, null);
    }

    private void afterClaim(String message) {
        messageLabel.setText(message);
        refresh();
        if (onChanged != null)
            onChanged.run();
    }

    /** 一覧の作り直し(受け取れるもの → 次の目標 → 受取済み(新しい順)) */
    private void refresh() {
        int total = StarRewardService.totalStars();
        int iv = StarRewardService.interval();
        int mc = StarRewardService.milestoneCount();
        summaryLabel.setText("★ " + total + " / " + StarRewardService.nextTarget());

        List<Integer> claimable = new ArrayList<>();
        List<Integer> claimed = new ArrayList<>();
        for (int n = 1; n <= mc; n++) {
            if (StarRewardService.isClaimed(n))
                claimed.add(0, n);
            else
                claimable.add(n);
        }
        listPanel.removeAll();
        for (int n : claimable)
            listPanel.add(row(n, total, iv));
        for (int n = mc + 1; n <= mc + UPCOMING; n++)
            listPanel.add(row(n, total, iv));
        for (int n : claimed)
            listPanel.add(row(n, total, iv));
        claimAllBtn.setEnabled(StarRewardService.claimableCount() > 0);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel row(int n, int total, int iv) {
        int stars = n * iv;
        JPanel row = new JPanel(new BorderLayout(px(6), 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 160, 160), 1),
                BorderFactory.createEmptyBorder(px(4), px(6), px(4), px(6))));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, px(54)));
        row.setPreferredSize(new Dimension(px(300), px(54)));

        JLabel starLabel = new JLabel("★" + stars);
        starLabel.setForeground(Color.YELLOW);
        starLabel.setFont(starLabel.getFont().deriveFont(Font.BOLD, (float) (15 * adj)));
        starLabel.setPreferredSize(new Dimension(px(58), px(40)));
        row.add(starLabel, BorderLayout.WEST);

        JLabel rewardLabel = new JLabel("<html>" + rewardText(StarRewardService.rewardOf(n)) + "</html>");
        rewardLabel.setForeground(Color.WHITE);
        rewardLabel.setFont(rewardLabel.getFont().deriveFont((float) (12 * adj)));
        row.add(rewardLabel, BorderLayout.CENTER);

        int stateW = px(84);
        if (StarRewardService.isClaimed(n)) {
            row.add(stateLabel("受取済み", Color.GRAY, stateW), BorderLayout.EAST);
        } else if (StarRewardService.canClaim(n)) {
            JButton btn = textBtn("受け取る", stateW, px(36));
            btn.addActionListener(e -> {
                Reward r = StarRewardService.claim(n);
                if (r != null)
                    afterClaim("★" + stars + " の報酬を受け取りました");
            });
            row.add(btn, BorderLayout.EAST);
        } else {
            row.add(stateLabel("未達成 あと" + (stars - total), Color.LIGHT_GRAY, stateW), BorderLayout.EAST);
        }
        return row;
    }

    private JLabel stateLabel(String text, Color color, int width) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont((float) (11 * adj)));
        l.setPreferredSize(new Dimension(width, px(36)));
        return l;
    }

    private JButton textBtn(String text, int w, int h) {
        return UIBuilder.createImageBtn(component, (BufferedImage) null, new Vector2(w, h), text);
    }

    /** 報酬の表示文(コインと素材名×数) */
    static String rewardText(Reward r) {
        StringBuilder sb = new StringBuilder();
        if (r.coins() > 0)
            sb.append("コイン×").append(r.coins());
        if (r.skipTickets() > 0)
            sb.append(sb.length() > 0 ? "  " : "").append("研究スキップ券×").append(r.skipTickets());
        for (Map.Entry<ResourceKinds, Integer> e : r.resources().entrySet()) {
            if (sb.length() > 0)
                sb.append("  ");
            sb.append(e.getKey().getResName().getDisplayName()).append("×").append(e.getValue());
        }
        return sb.length() == 0 ? "なし" : sb.toString();
    }

    private int px(double v) {
        return (int) (v * adj);
    }
}
