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
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.damage.DamagePopupManager;
import com.game.shooting_game.gameScene.GameSceneUI.GameSceneUIData;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.gameScene.stage.EndlessWaves;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.gameScene.subPanels.PausePanel;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.model.StageRewards;
import com.game.shooting_game.domain.module.ModuleKind;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.skill.SkillService;
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
    private final BeamManager BEAM_M;

    // 船のモード
    public static enum ColorMode {
        RED,
        BLUE
    }

    private String animationName = ""; // アニメーション名
    private double animateTimer = 0; // アニメーション用タイマー
    private boolean gameover;
    @Setter
    private boolean pause;
    // setup()が終わるまでupdate()を進めない(途中で進むと、前回の状態のまま船が動いて弾を撃ってしまう)
    private volatile boolean setupDone = false;
    @Getter
    @Setter
    private int killCountSinceLastLevelUp; // 最近のレベルアップドロップからのキル数
    private int score;
    private int coin;
    private int hitCount; // このステージで受けた被弾の回数(HARDのミッション用)
    private double playTime; // このステージの経過時間(秒。ポーズ中は進まない)
    private boolean skipped; // スキップで報酬だけ受け取ったか(ミッションの対象外)
    private final Map<ResourceKinds, Integer> collectedResources = new LinkedHashMap<>(); // このステージで拾った素材

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
        BEAM_M = BeamManager.getInstance();

        managers.add(STAGE_M);
        managers.add(BM);
        managers.add(BEAM_M); // 船より後ろに描くので船より前に置く
        managers.add(SHIP_M);
        managers.add(IM);
        managers.add(DamagePopupManager.getInstance()); // ダメージ数字は一番手前に描く

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
        setupDone = false;
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + "Stage " + STAGE_M.getStageId() + ANSI.RESET + " loaded.");

        // ステージ条件の読み込み(敵が硬い・回復なし など。プレイヤーの生成より前に行う)
        StageData sd = STAGE_M.getStageConfig().get("stage" + STAGE_M.getStageId());
        StageModifiers.load(sd == null ? null : sd.getModifiers(), STAGE_M.getStage().getDifficulty());

        // リセット
        gameover = false;
        killCountSinceLastLevelUp = 0;
        score = 0;
        coin = 0;
        hitCount = 0;
        playTime = 0;
        skipped = false;
        collectedResources.clear();

        // 各マネージャーのセットアップ
        for (Manager m : managers) {
            m.setup();
        }

        animation("start");
        setupDone = true;
    }

    @Override
    public void update(double dt) {
        if (!setupDone)
            return;
        if (!pause) {
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
                    playTime += dt;
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
                default -> {
                }
            }

            // 各マネージャーのアップデート
            for (Manager m : managers) {
                m.update(dt);
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
     * 被弾の記録(自機がダメージを受けたとき)
     */
    public void addHit() {
        hitCount++;
    }

    /**
     * スキップ(遊ばずに、クリア済みの難易度の報酬だけ受け取る)
     */
    public void skipResult() {
        skipped = true;
        result();
    }

    /**
     * モジュール報酬のレア度を難易度に応じて上げる(最大はLEGENDARY)
     */
    private static Rarity bumpRarity(Rarity rarity, int bump) {
        Rarity[] all = Rarity.values();
        return all[Math.min(all.length - 1, rarity.ordinal() + bump)];
    }

    /**
     * リザルト処理
     */
    public void result() {
        // 無限モードはゲームオーバーまで続くので、終わったときに到達したウェーブに応じた報酬を出す
        boolean endless = STAGE_M.getStageId() == EndlessWaves.STAGE_ID;
        boolean isClear = endless || getGameResult();

        // 結果ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + (isClear ? "Game Clear" : "Game Over") + ANSI.RESET);

        if (isClear) {
            UserData data = DataManager.getUserData();

            // 新しくクリアしたステージの場合更新
            // (エクストラステージは番号が100以上なので、通常ステージの進行度には数えない)
            if (STAGE_M.getStageId() < 100 && STAGE_M.getStageId() > data.getProgress().getClearedStage()) {
                data.getProgress().setClearedStage(STAGE_M.getStageId());

                // クリア進捗更新ログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Progress reached " + ANSI.YELLOW + "Stage" + STAGE_M.getStageId() + ANSI.RESET + ".");
            }

            StageRewards.getInstance().resetRewards(); // 報酬キャッシュのリセット

            // 経験値加算
            data.getProgress().addExp(score / 10);

            // 初クリアかどうか(最高スコアの更新より前に調べる)
            // (難易度ごとに別の記録。初クリアの報酬も難易度ごとに1回ずつ)
            Difficulty diff = STAGE_M.getStage().getDifficulty();
            String scoreKey = DifficultyService.key(STAGE_M.getStageId(), diff);
            boolean firstClear = !data.getProgress().getHighScores().containsKey(scoreKey);

            // 最高スコアの更新
            data.getProgress().updateHighScore(scoreKey, score);
            if (!data.getProgress().getHighScores().containsKey(scoreKey))
                data.getProgress().getHighScores().put(scoreKey, 0); // スコア0でもクリア済みとして残す
            StageRewards.getInstance().setScore(score);
            StageRewards.getInstance().setDifficultyLabel(diff == Difficulty.NORMAL ? "" : diff.getLabel());

            // HARD のミッション(スキップでは判定しない)
            if (diff == Difficulty.HARD && !skipped && !endless) {
                double hpRate = SHIP_M.getPlayer().getMaxHp() > 0
                        ? SHIP_M.getPlayer().getHp() / SHIP_M.getPlayer().getMaxHp()
                        : 0;
                StageRewards.getInstance().getNewMissions().addAll(
                        DifficultyService.recordMissions(STAGE_M.getStageId(), hitCount, hpRate, playTime));
            }

            // コインの加算(難易度で増える)
            int coinGain = (int) Math.round(coin * diff.getRewardRate());
            data.getResources().setCoins(data.getResources().getCoins() + coinGain);
            StageRewards.getInstance().getResources().put(ResourceKinds.COIN, coinGain);

            // ステージ中に拾った素材をバックパックに追加
            for (Map.Entry<ResourceKinds, Integer> e : collectedResources.entrySet()) {
                int owned = data.getResources().getEquipments().getOrDefault(e.getKey(), 0);
                data.getResources().getEquipments().put(e.getKey(), owned + e.getValue());
                StageRewards.getInstance().getResources().merge(e.getKey(), e.getValue(), Integer::sum);
            }

            // 報酬内容を取得
            StageData currentStageData = STAGE_M.getStageConfig().get("stage" + STAGE_M.getStageId());
            // バックパックに報酬を追加
            for (Map.Entry<ResourceName, Map<Rarity, String>> list : currentStageData.getRewardList().entrySet()) {
                ResourceName resName = list.getKey();
                Map<Rarity, String> rarityMap = list.getValue();

                for (Map.Entry<Rarity, String> rarityEntry : rarityMap.entrySet()) {
                    Rarity rarity = rarityEntry.getKey();

                    int amount = (int) Math.round(
                            currentStageData.getRandomResourceAmount(new ResourceKinds(resName, rarity))
                                    * diff.getRewardRate());
                    ResourceKinds resKind = new ResourceKinds(resName, rarity);
                    int amountOwned = data.getResources().getEquipments().getOrDefault(resKind, 0);

                    data.getResources().getEquipments().put(resKind, amountOwned + amount);
                    StageRewards.getInstance().getResources().merge(resKind, amount, Integer::sum);
                }
            }

            // 無限モード: 突破したウェーブ数に応じた報酬と、最高到達ウェーブの記録
            if (endless) {
                int cleared = Math.max(0, STAGE_M.getStage().getWaveId() - 1);
                if (cleared > data.getProgress().getHighScores().getOrDefault(EndlessWaves.BEST_WAVE_KEY, 0))
                    data.getProgress().getHighScores().put(EndlessWaves.BEST_WAVE_KEY, cleared);
                for (Map.Entry<ResourceKinds, Integer> e : EndlessWaves.materialRewards(cleared).entrySet()) {
                    data.getResources().getEquipments().merge(e.getKey(), e.getValue(), Integer::sum);
                    StageRewards.getInstance().getResources().merge(e.getKey(), e.getValue(), Integer::sum);
                }
                for (Rarity rarity : EndlessWaves.moduleRewards(cleared))
                    StageRewards.getInstance().getModules().add(ModuleService.addNew(null, rarity));
            }

            // モジュールの報酬(確率で手に入る。種類を決めていなければランダム)
            if (currentStageData.getModuleRewards() != null) {
                String dailyKey = "daily:" + java.time.LocalDate.now() + ":stage" + STAGE_M.getStageId();
                for (StageManager.ModuleReward r : currentStageData.getModuleRewards()) {
                    // 初クリアだけ・1日1回、の指定
                    if ("FIRST".equals(r.getOnce()) && !firstClear)
                        continue;
                    if ("DAILY".equals(r.getOnce()) && data.getProgress().getHighScores().containsKey(dailyKey))
                        continue;
                    if (Math.random() >= Math.min(1.0, r.getChance() * diff.getModuleChanceRate()))
                        continue;
                    try {
                        ModuleKind kind = r.getKind() == null ? null : ModuleKind.valueOf(r.getKind());
                        StageRewards.getInstance().getModules().add(ModuleService
                                .addNew(kind, bumpRarity(Rarity.valueOf(r.getRarity()), diff.getRarityBump())));
                    } catch (IllegalArgumentException e) {
                        LogUtils.systemLog("[WARN]", component, "Invalid module reward: " + r);
                    }
                }
                // 1日1回の報酬を受け取ったことを記録する
                for (StageManager.ModuleReward r : currentStageData.getModuleRewards())
                    if ("DAILY".equals(r.getOnce()))
                        data.getProgress().getHighScores().put(dailyKey, 1);
            }
        }
        SHIP_M.getPlayer().removeHandler();

        SM.changeScene(SceneType.GAMERESULT);

    }

    private static final double SAME_COLOR_DAMAGE_RATE = 0.5; // 同じ色の弾を受けたときのダメージ倍率

    /**
     * 同じ色の敵に与えるダメージの倍率(スキルで引き上げ)
     */
    public double sameColorDamageRate() {
        return SAME_COLOR_DAMAGE_RATE + Math.min(0.125, SkillService.getBonus("SAME_COLOR"));
    }

    /**
     * スキルを反映した自機弾のダメージ
     */
    public double playerBulletDamage(Bullet bullet) {
        return bullet.getDamage() * DamageCalculator.bulletAttackRate();
    }

    /**
     * 拾った素材の記録(ステージクリア時にバックパックへ入る)
     * 
     * @param resKind 素材
     */
    public void addResource(ResourceKinds resKind) {
        collectedResources.merge(resKind, 1, Integer::sum);
    }

    /**
     * 当たり判定処理
     */
    public void checkCollisions() {
        if (SHIP_M.getPlayer() == null)
            return;

        // 全敵リストの作成
        List<Ship> allEnemies = new ArrayList<>();
        allEnemies.addAll(SHIP_M.getEnemyList());
        allEnemies.addAll(SHIP_M.getBossList());

        // 船体衝突
        for (Ship enemy : allEnemies) {
            // モードが同じなら無視(ボスは色の概念がないので常に接触する)
            if (!enemy.isColorless() && SHIP_M.getPlayer().getMode() == enemy.getMode())
                continue;

            if (enemy.collidesWithCircle(SHIP_M.getPlayer().getPos(), SHIP_M.getPlayer().getCollisionSize())) {
                // プレイヤーが無敵時間中でなければ(今回の衝突でプレイヤーにダメージが入るなら)ボスにもダメージ
                boolean playerVulnerable = SHIP_M.getPlayer().getShieldTimer() <= 0;

                switch (enemy.getOwner()) {
                    // アイテムをドロップせずに倒す(coinやスコアは入る)
                    case ShipKinds.ENEMY -> enemy.kill(false);
                    // ボスにダメージを与える(1回の衝突につき1度だけ)
                    case ShipKinds.BOSS -> {
                        if (playerVulnerable)
                            enemy.addDamage(enemy.getRamDamage());
                    }
                    // 何もしない
                    default -> {
                    }
                }
                SHIP_M.getPlayer().addDamage(DamageCalculator.ramToPlayer(SHIP_M.getPlayer())); // プレイヤーにダメージを与える
            }
        }

        // 弾衝突
        for (Bullet bullet : BM.getBulletList()) {
            if (bullet.getOwner() == ShipKinds.PLAYER) {
                for (Ship enemy : allEnemies) {

                    if (enemy.collidesWithCircle(bullet.getPos(), bullet.getCollisionSize()) && enemy.inScreen()) {
                        // 貫通弾は同じ敵に2度当たらない
                        if (!bullet.registerHit(enemy))
                            continue;

                        // どの色の弾でもダメージを与える(同じ色の場合は減衰、ボスは色の概念がない)
                        double damage = DamageCalculator.bulletToEnemy(bullet, enemy);

                        enemy.addDamage(damage);

                        // 貫通数が残っていれば通り抜ける
                        if (bullet.consumeHit()) {
                            bullet.destroySelf(); // 弾を削除
                            break;
                        }
                    }
                }
            } else {
                Player player = SHIP_M.getPlayer();
                if (GameUtils.collidesWith(player, bullet)) {
                    bullet.destroySelf(); // 弾を削除

                    // モードが同じなら無視
                    if (bullet.getMode() == player.getMode())
                        continue;

                    SHIP_M.getPlayer().addDamage(DamageCalculator.toPlayer(bullet.getDamage()));
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
        coin += (int) Math.round(value * (1 + SkillService.getBonus("COIN_BONUS")) * StageModifiers.coinRate());
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