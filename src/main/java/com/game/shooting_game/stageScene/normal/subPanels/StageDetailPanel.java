package com.game.shooting_game.stageScene.normal.subPanels;

import static com.game.shooting_game.ui.UIStyle.px;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.game.shooting_game.ResourceManager.ResourceKinds;
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
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class StageDetailPanel extends JPanel {
    private static final String component = "[StageDetail]";

    private final SceneManager SM;
    private final GameManager GM;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

    private Vector2 panelSize; // ステージ詳細パネルの大きさ
    private int spacing; // 外周のスペース

    private StageInfoView infoView;
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

        // スクロール可能な詳細(ハイスコア・報酬・出現する敵)
        int innerWidth = (int) panelSize.getX() - 2 * spacing;
        infoView = new StageInfoView(innerWidth - px(8));
        JScrollPane scrollPane = new JScrollPane(infoView);
        scrollPane.setBorder(null); // 境界線を消す
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(px(16));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBounds(
                spacing, spacing,
                innerWidth,
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

        // 詳細情報の設定(報酬は素材名→レア度の順に並べる)
        List<StageInfoView.Reward> rewards = new ArrayList<>();
        for (Map.Entry<ResourceName, Map<Rarity, String>> list : getRewardList(stageId).entrySet()) {
            for (Map.Entry<Rarity, String> rarityEntry : list.getValue().entrySet())
                rewards.add(new StageInfoView.Reward(new ResourceKinds(list.getKey(), rarityEntry.getKey()),
                        rarityEntry.getValue()));
        }
        int highScore = DataManager.getUserData().getProgress().getHighScores().getOrDefault("stage" + stageId, 0);
        infoView.setData(highScore, rewards, getEnemyList(stageId));

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
