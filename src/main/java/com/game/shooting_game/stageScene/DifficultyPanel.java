package com.game.shooting_game.stageScene;

import static com.game.shooting_game.ui.UIStyle.px;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.gameScene.stage.DifficultyService.Mission;
import com.game.shooting_game.ui.UIStyle;

/**
 * 難易度の選択(EASY / NORMAL / HARD / NIGHTMARE)
 * 選べない難易度は押せず、解放条件を表示する。NIGHTMARE は HARD のミッションの達成状況も出す。
 * 通常ステージの詳細と、エクストラのボス分類の詳細で使う。
 */
public class DifficultyPanel extends JPanel {
    private static final Color SELECTED = new Color(110, 210, 255);

    private final Map<Difficulty, JButton> buttons = new EnumMap<>(Difficulty.class);
    private final JLabel info = new JLabel();
    private final Consumer<Difficulty> onChange;
    private int stageId;
    private Difficulty selected = Difficulty.NORMAL;

    /**
     * @param width    パネルの幅
     * @param onChange 難易度が選ばれたときの処理
     */
    public DifficultyPanel(int width, Consumer<Difficulty> onChange) {
        this.onChange = onChange;
        setLayout(null);
        setOpaque(false);
        setSize(width, height());

        Difficulty[] all = Difficulty.values();
        int gap = px(3);
        int bw = (width - gap * (all.length - 1)) / all.length;
        for (int i = 0; i < all.length; i++) {
            Difficulty d = all[i];
            JButton b = UIStyle.button(d.getLabel(), 10);
            b.setBounds(i * (bw + gap), 0, bw, px(28));
            b.addActionListener(e -> select(d));
            add(b);
            buttons.put(d, b);
        }

        info.setForeground(Color.WHITE);
        info.setFont(UIStyle.font(11, false));
        info.setVerticalAlignment(JLabel.TOP);
        info.setBounds(px(2), px(32), width - px(4), height() - px(32));
        add(info);
    }

    public static int height() {
        return px(112);
    }

    public Difficulty getSelected() {
        return selected;
    }

    /**
     * ステージを開いたときに表示する(NORMALを選んだ状態から始まる)
     */
    public void show(int stageId) {
        this.stageId = stageId;
        select(Difficulty.NORMAL);
    }

    private void select(Difficulty d) {
        selected = d;
        refreshButtons();
        onChange.accept(d);
    }

    private void refreshButtons() {
        for (Map.Entry<Difficulty, JButton> e : buttons.entrySet()) {
            Difficulty d = e.getKey();
            JButton b = e.getValue();
            boolean open = DifficultyService.unlocked(stageId, d);
            b.setEnabled(open);
            b.setText(d == selected ? "【" + d.getLabel() + "】" : d.getLabel());
            b.setForeground(d == selected ? SELECTED : null);
        }
        info.setText(infoText());
    }

    private boolean isBossStage() {
        var sd = com.game.shooting_game.gameScene.stage.StageManager.getInstance().getStageConfig().get("stage" + stageId);
        return sd != null && "BOSS".equals(sd.getCategory());
    }

    private String infoText() {
        StringBuilder sb = new StringBuilder("<html>").append(isBossStage() ? selected.describeBoss() : selected.describe());
        // 次の難易度が解放されていない間は、その条件を見せる
        Difficulty next = selected == Difficulty.NORMAL ? Difficulty.HARD
                : selected == Difficulty.HARD ? Difficulty.NIGHTMARE : null;
        if (selected == Difficulty.HARD) {
            sb.append("<br><font color='#9fd8ff'>ナイトメア解放ミッション:</font>");
            for (Mission m : DifficultyService.missions(stageId))
                sb.append("<br>").append(m.done() ? "<font color='#ffd650'>★</font> " : "<font color='#999999'>☆</font> ").append(m.text());
        } else if (next != null && !DifficultyService.unlocked(stageId, next)) {
            sb.append("<br><font color='#9fd8ff'>").append(next.getLabel()).append(": ")
                    .append(DifficultyService.lockReason(stageId, next)).append("</font>");
        }
        return sb.append("</html>").toString();
    }
}
