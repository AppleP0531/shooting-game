package com.game.shooting_game.stageScene.normal.subPanels;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class StageDetailPanel extends JPanel {
    private static final String component = "[StageDetail]";

    private final SceneManager SM;
    private final GameManager GM;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

    private Vector2 panelSize; // ステージ詳細パネルの大きさ
    private int spacing; // 外周のスペース

    private JTextArea detailTextArea;
    private JButton startStageBtn;
    private JButton skipStageBtn;

    public StageDetailPanel() {
        SM = SceneManager.getInstance();
        GM = GameManager.getInstance();
        NSM = NormalStageManager.getInstance();
        STAGE_M = StageManager.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        panelSize = new Vector2(350 * ShootingGameMain.getScreenSizeAdj(),
                480 * ShootingGameMain.getScreenSizeAdj());
        setBounds(
                NSM.getStageBtnPanel().getWidth() / 2 - (int) panelSize.getX() / 2,
                NSM.getStageBtnPanel().getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());
        spacing = (int) (5 * ShootingGameMain.getScreenSizeAdj());

        // スクロール可能な詳細テキストエリアの追加
        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true); // 自動改行
        detailTextArea.setWrapStyleWord(true); // 単語単位で改行
        detailTextArea.setOpaque(false);
        detailTextArea.setForeground(Color.WHITE);
        detailTextArea.setCaretColor(new Color(0, 0, 0, 0)); // カーソルの色を透明にする
        // 文字サイズの指定
        double fontSize = 14 * ShootingGameMain.getScreenSizeAdj();
        detailTextArea.setFont(new Font("SansSerif", Font.PLAIN, (int) fontSize));
        JScrollPane scrollPane = new JScrollPane(detailTextArea);
        scrollPane.setBorder(null); // 境界線を消す
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBounds(
                spacing, spacing,
                (int) panelSize.getX() - 2 * spacing,
                (int) (panelSize.getY() - 3 * spacing - 50 * ShootingGameMain.getScreenSizeAdj()));
        this.add(scrollPane);

        // スタートボタン(ゲーム画面に遷移)
        Vector2 startStageBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String startStageImagePath = "/static/img/UI/lobbyScene/startStage.png";
        startStageBtn = UIBuilder.createImageBtn(component, startStageImagePath, startStageBtnSize,
                "START");
        startStageBtn.addActionListener(e -> {
            UIBuilder.closePanel(this);

            SM.changeScene(SceneType.GAME);
        });
        this.add(startStageBtn);

        // スキップボタン(ステージスキップして報酬を入手)
        Vector2 skipStageBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String skipStageImagePath = "/static/img/UI/lobbyScene/skipStageImage.png";
        skipStageBtn = UIBuilder.createImageBtn(component, skipStageImagePath, skipStageBtnSize,
                "SKIP");
        skipStageBtn.setBounds(
                spacing,
                (int) panelSize.getY() - (int) skipStageBtn.getHeight() - spacing,
                (int) skipStageBtn.getWidth(), (int) skipStageBtn.getHeight());
        skipStageBtn.addActionListener(e -> {
            SM.changeScene(SceneType.GAME);
            GM.result();
            UIBuilder.closePanel(this);
        });
        this.add(skipStageBtn);
    }

    /**
     * ステージ詳細パネルの表示
     * 
     * @param isCleared クリア済みステージかどうか
     */
    public void showStageDaetail(int stageId) {
        drawBtn(stageId);

        // 詳細情報の設定
        String detailText = "";
        // 報酬内容
        detailText += "Rewards\n";
        for (Map.Entry<ResourceName, Map<Rarity, String>> list : getRewardList(stageId).entrySet()) {
            ResourceName resName = list.getKey();
            Map<Rarity, String> rarityMap = list.getValue();

            for (Map.Entry<Rarity, String> rarityEntry : rarityMap.entrySet()) {
                Rarity rarity = rarityEntry.getKey();
                String amount = rarityEntry.getValue();
                detailText += resName + " " + rarity + ": " + amount + "\n";
            }
        }
        detailText += "\n";
        // 出現する敵
        detailText += "Enemies\n";
        for (String enemyId : getEnemyList(stageId)) {
            detailText += enemyId + " , ";
        }
        detailText += "\n\n";
        // 最高スコア
        detailText += "High Score\n";
        detailText += DataManager.getUserData().getProgress().getHighScores().getOrDefault("stage" + stageId, 0);

        detailTextArea.setText(detailText);
        detailTextArea.setCaretPosition(0); // テキストエリアのスクロール位置を先頭にする

        UIBuilder.openPanel(this, "Stage" + stageId, true, null);
    }

    /**
     * ボタンの描画
     * 
     * @param stageId ステージID
     */
    private void drawBtn(int stageId) {
        // 現在のクリア状況の取得
        int clearedStage = DataManager.getUserData().getProgress().getClearedStage();
        // 選択ステージがクリア済みかどうか
        boolean isCleared = stageId <= clearedStage;
        // ステージをクリアしている場合はSTARTとSKIPを並べる、そうでなければSTARTを中央下に置く
        if (isCleared) {
            // スタートボタン
            startStageBtn.setBounds(
                    (int) panelSize.getX() - (int) startStageBtn.getWidth() - spacing,
                    (int) panelSize.getY() - (int) startStageBtn.getHeight() - spacing,
                    (int) startStageBtn.getWidth(), (int) startStageBtn.getHeight());
            skipStageBtn.setVisible(true);
        } else {
            // スタートボタン
            startStageBtn.setBounds(
                    (int) panelSize.getX() / 2 - (int) startStageBtn.getWidth() / 2,
                    (int) panelSize.getY() - (int) startStageBtn.getHeight() - spacing,
                    (int) startStageBtn.getWidth(), (int) startStageBtn.getHeight());
            skipStageBtn.setVisible(false);
        }
    }

    /**
     * 報酬情報の取得
     * 
     * @param stageId ステージID
     * @return 報酬内容
     */
    private Map<ResourceName, Map<Rarity, String>> getRewardList(int stageId) {
        StageData stageData = STAGE_M.getStageConfig().get("stage" + stageId);

        try {
            return new LinkedHashMap<>(stageData.getRewardList());
        } catch (NullPointerException e) {
            // 報酬情報の取得に失敗した場合
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Failed to get reward list for Stage" + ANSI.YELLOW + stageId + ANSI.RESET + ".");
        }
        return new LinkedHashMap<>();
    }

    /**
     * 敵リストの取得
     * 
     * @param stageId ステージID
     * @return 敵リスト
     */
    private List<String> getEnemyList(int stageId) {
        StageData stageData = STAGE_M.getStageConfig().get("stage" + stageId);
        List<String> enemyList = new ArrayList<>();

        try {
            for (Map.Entry<String, EnemyConfig[]> waveEntry : stageData.getEnemyList().entrySet()) {
                EnemyConfig[] enemyData = waveEntry.getValue();

                for (EnemyConfig config : enemyData) {
                    String enemyId = config.getEnemyId();
                    if (!enemyList.contains(enemyId))
                        enemyList.add(enemyId);
                }
            }
        } catch (NullPointerException e) {
            return enemyList;
        }

        return enemyList;
    }
}
