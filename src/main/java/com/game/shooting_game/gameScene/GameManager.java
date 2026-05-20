package com.game.shooting_game.gameScene;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.DataManager.UserData;
import com.game.shooting_game.gameScene.GameSceneUI.GameSceneUIData;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.gameScene.subPanels.PausePanel;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

public final class GameManager extends JPanel implements Manager {
    private static final String component = "[GameManager]";
    private static GameManager instance;
    private final GameSceneUI gameSceneUI;
    private final PausePanel pausePanel;

    private final SceneManager SM;
    private final ShipManager SHIP_M;
    private final StageManager STAGE_M;
    private final BulletManager BM;
    private final ItemManager IM;

    private String animationName = "none"; // アニメーション名
    private double animateTimer = 0; // アニメーション用タイマー
    private boolean gameover;
    @Setter
    private boolean pause;
    @Getter
    @Setter
    private int killCountSinceLastLevelUp; // 最近のレベルアップドロップからのキル数
    private int score;
    private int coin;

    private final List<Manager> managers = new ArrayList<>();

    private Map<String, Double> effectList; // プレイヤーが所有するエフェクトのマップ

    private GameManager() {
        GameManager.instance = this;
        gameSceneUI = new GameSceneUI();
        pausePanel = new PausePanel();
        pausePanel.initComponents();

        SM = SceneManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        STAGE_M = StageManager.getInstance();
        BM = BulletManager.getInstance();
        IM = ItemManager.getInstance();

        managers.add(STAGE_M);
        managers.add(BM);
        managers.add(SHIP_M);
        managers.add(IM);

        effectList = new LinkedHashMap<>();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(pausePanel);

        // KeyBindings の設定
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // P、ESCキーでコマンドパネルを開く
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "togglePause");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // コマンド入力中なら無視
                if (ShootingGameMain.getGlassPane().isVisible())
                    return;

                // ポーズ状態でなければポーズする
                if (!pause) {
                    pause = true;
                    pausePanel.showPausePanel();
                }
            }
        });
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static GameManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, GameManager::new);
    }

    @Override
    public void setup() {
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + "Stage " + STAGE_M.getStageId() + ANSI.RESET + " loaded.");

        // リセット
        gameover = false;
        killCountSinceLastLevelUp = 0;
        score = 0;
        coin = 0;

        // 各マネージャーのセットアップ
        for (Manager m : managers) {
            m.setup();
        }
        SHIP_M.getPlayer().addHandler();

        animation("start");
    }

    @Override
    public void update(double dt) {
        if (!pause) {
            // 各マネージャーのアップデート
            for (Manager m : managers) {
                m.update(dt);
            }

            switch (animationName) {
                case "start" -> {
                    SHIP_M.getPlayer().animation("admission");
                    animation("countdown");
                }
                case "countdown" -> {
                    animateTimer += dt;
                    if (4 < animateTimer) {
                        SHIP_M.getPlayer().animation("none");
                        animation("none");
                    }
                }
                case "none" -> {
                    checkCollisions();

                    // プレイヤーのHPがなくなったらゲームオーバーフラグを立ててリザルト画面に遷移
                    if (SHIP_M.getPlayer().isDead()) {
                        // テストシーンではない場合はゲームオーバー処理、テストシーンならプレイヤーセットアップ
                        if (SM.getSceneName() != SceneType.TEST) {
                            gameover = true;
                            SHIP_M.getPlayer().animation("death");
                            animation("finish");
                        } else
                            SHIP_M.setup();
                    }
                }
                case "finish" -> {

                }
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        updateEffectData();

        // 各マネージャーの描画
        for (Manager m : managers) {
            m.draw(g);
        }

        // 必要データのパッキング
        GameSceneUIData uiData = new GameSceneUIData(
                animationName,
                animateTimer,
                score,
                coin,
                effectList);
        gameSceneUI.draw(g, uiData);
    }

    private void animation(String name) {
        animateTimer = 0;
        animationName = name;
    }

    /**
     * 獲得中エフェクトの更新
     * 
     * @param g
     */
    private void updateEffectData() {
        effectList.compute("OOC",
                (k, v) -> SHIP_M.getPlayer().getOOCTimer() > 0 ? SHIP_M.getPlayer().getOOCTimer() : null);
        effectList.compute("Shield",
                (k, v) -> SHIP_M.getPlayer().getShieldTimer() > 0 || SHIP_M.getPlayer().getDamagedTimer() > 0
                        ? Math.max(SHIP_M.getPlayer().getShieldTimer(), SHIP_M.getPlayer().getDamagedTimer())
                        : null);
    }

    /**
     * リザルト処理
     */
    public void result() {
        boolean isClear = getGameResult();

        // 結果ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + (isClear ? "Game Clear" : "Game Over") + ANSI.RESET);

        if (isClear) {
            UserData data = DataManager.getUserData();

            // 新しくクリアしたステージの場合更新
            if (STAGE_M.getStageId() > data.getProgress().getClearedStage()) {
                data.getProgress().setClearedStage(STAGE_M.getStageId());

                // クリア進捗更新ログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Progress reached " + ANSI.YELLOW + "Stage" + STAGE_M.getStageId() + ANSI.RESET + ".");
            }

            // 報酬内容を取得
            StageData currentStageData = STAGE_M.getStageConfig().get("stage" + STAGE_M.getStageId());
            // バックパックに報酬を追加
            for (ResourceKinds resName : currentStageData.getRewardList().keySet()) {
                int amount = currentStageData.getRandomResourceAmount(resName);
                int amountOwned = data.getResources().getEquipment().getOrDefault(resName, 0);
                data.getResources().getEquipment().put(resName, amountOwned + amount);
            }

            // コインの加算
            data.getResources().setCoins(data.getResources().getCoins() + coin);

            // 最高スコアの更新
            data.getProgress().updateHighScore("stage" + STAGE_M.getStageId(), score);

            // 経験値加算
            data.getProgress().addExp(score / 10);
        }
        SHIP_M.getPlayer().removeHandler();

        SM.changeScene(SceneType.GAMERESULT);
    }

    /**
     * 当たり判定処理
     */
    public void checkCollisions() {
        // 全敵リストの作成
        List<Ship> allEnemies = new ArrayList<>();
        allEnemies.addAll(SHIP_M.getEnemyList());
        allEnemies.addAll(SHIP_M.getBossList());

        // 船体衝突
        for (Ship enemy : allEnemies) {
            if (SHIP_M.getPlayer() != null && GameUtils.collidesWith(SHIP_M.getPlayer(), enemy)) {
                switch (enemy.getOwner()) {
                    // アイテムをドロップせずに倒す(coinやスコアは入る)
                    case ShipKinds.ENEMY -> enemy.kill(false);
                    // ボスに10%のダメージを与える
                    case ShipKinds.BOSS -> enemy.addDamage(enemy.getMaxHp() * 0.1);
                    // 何もしない
                    default -> {
                    }
                }
                SHIP_M.getPlayer().addDamage(SHIP_M.getPlayer().getMaxHp() * 0.25); // プレイヤーにダメージを与える
            }
        }

        // 弾衝突
        for (Bullet bullet : BM.getBulletList()) {
            if (bullet.getOwner() == ShipKinds.PLAYER) {
                for (Ship enemy : allEnemies) {
                    if (GameUtils.collidesWith(enemy, bullet) && enemy.inScreen()) {
                        enemy.addDamage(bullet.getDamage());
                        bullet.destroySelf();
                        break;
                    }
                }
            } else {
                if (GameUtils.collidesWith(SHIP_M.getPlayer(), bullet)) {
                    SHIP_M.getPlayer().addDamage(bullet.getDamage());
                    bullet.destroySelf();
                    break;
                }
            }
        }

        // アイテム衝突
        for (Item item : IM.getItemList()) {
            if (GameUtils.collidesWith(SHIP_M.getPlayer(), item)) {
                item.effect();
                item.destroySelf();
            }
        }
    }

    /**
     * スコアの加算
     * 
     * @param value 値
     */
    public void addScore(double value) {
        score += value;
    }

    /**
     * コインの加算
     * 
     * @param value 値
     */
    public void addCoin(int value) {
        coin += value;
    }

    /**
     * ゲーム結果を返す
     * 
     * @return クリアしている場合true、そうでなければfalse
     */
    public boolean getGameResult() {
        return !gameover;
    }

    /**
     * ゲームオーバーにする
     * 
     * @param state trueでゲームオーバー
     */
    public void gameover(boolean state) {
        this.gameover = state;
    }
}