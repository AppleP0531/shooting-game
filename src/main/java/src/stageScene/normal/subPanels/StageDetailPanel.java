package src.stageScene.normal.subPanels;

import src.ResourceManager.ResourceKinds;
import src.SceneManager;
import src.ShootingGameMain;
import src.SceneManager.SceneType;
import src.data.DataManager;
import src.gameScene.GameManager;
import src.gameScene.stage.StageManager;
import src.gameScene.stage.StageManager.StageData;
import src.gameScene.stage.StageManager.EnemyConfig;
import src.stageScene.normal.NormalStageManager;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BorderFactory;

/**
 * ステージ詳細パネル描画クラス
 */
public class StageDetailPanel extends JPanel {
    private static final String component = "[StageDetail]";

    private final SceneManager SM;
    private final GameManager GM;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

    private Vector2 panelSize; // ステージ詳細パネルの大きさ
    private int spacing; // 外周のスペース

    private JButton startStageButton;
    private JButton skipStageButton;

    // コンストラクタ
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
        setBackground(new Color(0, 0, 0, 220));
        setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        setVisible(false);

        panelSize = new Vector2(350 * ShootingGameMain.getScreenSizeAdj(),
                480 * ShootingGameMain.getScreenSizeAdj());
        spacing = (int) (5 * ShootingGameMain.getScreenSizeAdj());

        // スタートボタン(ゲーム画面に遷移)
        Vector2 startStageButtonSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String startStageImagePath = "static/img/UI/lobbyScene/startStage.png";
        startStageButton = UIBuilder.createImageButton(component, startStageImagePath, startStageButtonSize,
                "START");
        startStageButton.addActionListener(e -> {
            setVisible(false);

            SM.changeScene(SceneType.GAME);
        });
        this.add(startStageButton);

        // スキップボタン(ステージスキップして報酬を入手)
        Vector2 skipStageButtonSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String skipStageImagePath = "static/img/UI/lobbyScene/skipStageImage.png";
        skipStageButton = UIBuilder.createImageButton(component, skipStageImagePath, skipStageButtonSize,
                "SKIP");
        skipStageButton.setBounds(
                spacing,
                (int) panelSize.getY() - (int) skipStageButton.getHeight() - spacing,
                (int) skipStageButton.getWidth(), (int) skipStageButton.getHeight());
        skipStageButton.addActionListener(e -> {
            SM.changeScene(SceneType.GAME);
            GM.result();
            setVisible(false);
        });
        this.add(skipStageButton);

        // 背後へのクリックを阻止
        MouseAdapter blocker = new MouseAdapter() {
        };
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
    }

    /**
     * ステージ詳細パネルの表示
     * 
     * @param isCleared クリア済みステージかどうか
     */
    public void showStageDaetail(int stageId) {
        setBounds(
                NSM.getStageButtonPanel().getWidth() / 2 - (int) panelSize.getX() / 2,
                NSM.getStageButtonPanel().getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        drawButton(stageId);

        // temporaryLogs 報酬内容をログに出力
        for (Map.Entry<ResourceKinds, String> list : getRewardList(stageId).entrySet()) {
            ResourceKinds resName = list.getKey();
            String amount = list.getValue();
            System.out.println(component + " : " + resName + ": " + amount);
        }
        // temporaryLogs 出現する敵をログに出力
        for (String enemyId : getEnemyList(stageId)) {
            System.out.println(component + " : " + enemyId);
        }

        UIBuilder.openPanel(this, "Stage" + stageId);
    }

    /**
     * ボタンの描画
     * 
     * @param stageId ステージID
     */
    private void drawButton(int stageId) {
        // 現在のクリア状況の取得
        int clearedStage = DataManager.getUserData().getProgress().getClearedStage();
        // 選択ステージがクリア済みかどうか
        boolean isCleared = stageId <= clearedStage;
        // ステージをクリアしている場合はSTARTとSKIPを並べる、そうでなければSTARTを中央下に置く
        if (isCleared) {
            // スタートボタン
            startStageButton.setBounds(
                    (int) panelSize.getX() - (int) startStageButton.getWidth() - spacing,
                    (int) panelSize.getY() - (int) startStageButton.getHeight() - spacing,
                    (int) startStageButton.getWidth(), (int) startStageButton.getHeight());
            skipStageButton.setVisible(true);
        } else {
            // スタートボタン
            startStageButton.setBounds(
                    (int) panelSize.getX() / 2 - (int) startStageButton.getWidth() / 2,
                    (int) panelSize.getY() - (int) startStageButton.getHeight() - spacing,
                    (int) startStageButton.getWidth(), (int) startStageButton.getHeight());
            skipStageButton.setVisible(false);
        }
    }

    /**
     * 報酬情報の取得
     * 
     * @param stageId ステージID
     * @return 報酬内容
     */
    private Map<ResourceKinds, String> getRewardList(int stageId) {
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
