package com.game.shooting_game.gameScene.stage;

import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import com.game.shooting_game.domain.farm.MaterialFarmService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.hazard.MeteorSpec;
import com.game.shooting_game.gameScene.beam.BlockSpec;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public final class StageManager implements Manager {
    private static final String component = "[StageManager]";
    private static StageManager instance;

    // ----- データクラスの定義 -----
    /**
     * クリア報酬のモジュール(種類を省略するとランダム)
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModuleReward {
        private String kind; // ATC / DEF / SHIELD / REPAIR / HP / HEAL / CRIT / CRITDMG(nullならランダム)
        private String rarity; // COMMON 〜 LEGENDARY
        private double chance = 1.0; // 手に入る確率(0〜1)
        private String once; // FIRST: そのステージの初クリアだけ / DAILY: 1日1回 / 省略: 毎回
    }

    /**
     * HARDのミッションの設定(省略した項目は標準値。全部達成でNIGHTMAREが解放される)
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MissionSpec {
        private int maxHits = 3; // 被弾回数の上限
        private double minHpRate = 0.5; // クリア時に残っているHPの割合
        private double secondsPerWave = 30; // 1ウェーブあたりの制限時間(秒)
        private double maxSeconds = 0; // 制限時間を直接指定(0なら上のウェーブ数から決める)
    }

    /**
     * ステージ情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class StageData {
        private String title; // エクストラステージの名前
        private String description; // エクストラステージの説明
        private String category; // エクストラ画面の分類: MODULE / BOSS / MATERIAL / SPECIAL
        private int unlock; // エクストラステージの解放条件(クリア済みのステージ番号)
        private List<ModuleReward> moduleRewards; // クリアで手に入るモジュール(確率つき)
        private MissionSpec missions; // HARDのミッション(省略で標準値)
        private StageModifiers.Spec modifiers; // ステージ条件(敵が硬い、回復なし など)
        private Map<ResourceName, Map<Rarity, String>> rewardList;
        private Map<String, EnemyConfig[]> enemyList;
        private Map<String, BlockSpec[]> blocks; // ウェーブごとのビームブロック(四角ブロック。流れながらビームを撃つ)
        private Map<String, BeamSpec[]> gimmicks; // ウェーブごとのビームギミック(壁・上からのビーム)
        private Map<String, MeteorSpec[]> meteors; // ウェーブごとの隕石(上から降ってくる障害物。自機の弾で壊せる)

        /**
         * 獲得リソース量の決定
         * 
         * @param resKind 獲得リソース名
         * @return 獲得量
         */
        public int getRandomResourceAmount(ResourceKinds resKind) {
            String range = rewardList.get(resKind.getResName()).get(resKind.getRarity());

            try {
                String[] num = range.split("~");
                int min = Integer.parseInt(num[0]);
                int max = Integer.parseInt(num[1]);

                return ThreadLocalRandom.current().nextInt(min, max + 1);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * スポーンする敵情報のクラス
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class EnemyConfig {
        private String enemyId;
        private ColorMode mode;
        private int level;
        private Vector2 spawnPos;
        private Vector2 stayPos;
        private EntranceType entrance; // 入場軌道(省略時: 敵は直線、ボスはBossConfigの指定)
        private String path; // 通る経路ID(指定すると経路を通る小敵になる。spawnPos/stayPosは不要)
        private boolean mirror; // 経路を左右反転するか
        private double delay; // 経路を動き出すまでの時間(縦列で出すときのずらし用)
        private double speedScale = 1.0; // 経路移動の速度倍率

        public ShipKinds getOwner() {
            Map<ShipKinds, Map<String, ShipDefinition>> shipConfig = ShipManager.getInstance().getShipConfig();

            for (Map.Entry<ShipKinds, Map<String, ShipDefinition>> entry : shipConfig.entrySet()) {
                if (entry.getValue().containsKey(enemyId)) {
                    return entry.getKey();
                }
            }
            throw new IllegalStateException(
                    "enemyId '" + enemyId + "' はShipConfig.jsonに存在しません。StageConfig.jsonの記述を確認してください。");
        }
    }

    private Map<String, StageData> stageConfigMap; // ステージデータが格納された呼び出し用のマップ
    private static final TypeReference<Map<String, StageData>> TYPE_REF = new TypeReference<>() {
    };

    @Getter
    private Stage stage;

    private StageManager() {
        StageManager.instance = this;
        stage = new Stage();
        reload();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static StageManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, StageManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/StageConfig.json";
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始ログ
            LogUtils.debug(component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            stageConfigMap = mapper.readValue(is, TYPE_REF);
            generateStages();

            // ロード成功ログ
            LogUtils.debug(component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.fatal(component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET + "\n"
                            + ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 31〜200の通常ステージを自動で作って加える(敵・ボスの種類は ShipConfig から集める)
     */
    private void generateStages() {
        Map<ShipKinds, Map<String, ShipDefinition>> cfg = ShipManager.getInstance().getShipConfig();
        List<String> enemies = new java.util.ArrayList<>();
        Map<String, ShipDefinition> en = cfg.get(ShipKinds.ENEMY);
        if (en != null)
            for (Map.Entry<String, ShipDefinition> e : en.entrySet())
                if (e.getValue().getDatas() != null)
                    enemies.add(e.getKey());
        List<String> bosses = new java.util.ArrayList<>();
        Map<String, ShipDefinition> bs = cfg.get(ShipKinds.BOSS);
        if (bs != null)
            for (String id : bs.keySet())
                if (id.matches("Boss\\d+"))
                    bosses.add(id);
        bosses.sort(java.util.Comparator.comparingInt(s -> Integer.parseInt(s.substring(4))));
        if (enemies.isEmpty())
            return;
        for (int id = StageGenerator.FIRST_GENERATED; id <= StageGenerator.LAST_STAGE; id++)
            stageConfigMap.put("stage" + id, StageGenerator.generate(id, enemies, bosses));
        generateFarmStages(enemies, bosses);
    }

    /**
     * 素材周回ステージ(素材ごとに1つ)を作る。中身は同じ価値の通常ステージと同じ作りで、ウェーブを絞り、報酬をその素材だけにする
     */
    private void generateFarmStages(List<String> enemies, List<String> bosses) {
        for (ResourceName res : MaterialFarmService.MATERIALS) {
            int unlock = MaterialFarmService.unlockStage(res);
            int base = Math.max(StageGenerator.FIRST_GENERATED, Math.min(StageGenerator.LAST_STAGE, unlock));
            if (base % 5 == 0)
                base++; // ボスのいないステージを土台にする
            StageData d = StageGenerator.generate(base, enemies, bosses);
            Map<String, EnemyConfig[]> waves = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, EnemyConfig[]> e : d.getEnemyList().entrySet()) {
                if (waves.size() >= MaterialFarmService.wavesOf())
                    break;
                waves.put(e.getKey(), e.getValue());
            }
            d.setEnemyList(waves);
            d.setRewardList(MaterialFarmService.rewards(res));
            d.setCategory("MATERIAL");
            d.setUnlock(unlock);
            d.setTitle(res.getDisplayName() + "の周回");
            d.setDescription(res.getDisplayName() + "を集めるための周回ステージ。\n手に入れたことのある素材だけが並ぶ。");
            stageConfigMap.put("stage" + MaterialFarmService.stageId(res), d);
        }
    }

    @Override
    public void setup() {
        stage.reset();
    }

    @Override
    public void update(double dt) {
        stage.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        stage.draw(g);
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
    }

    /**
     * ステージデータの取得
     * 
     * @return ステージデータ
     */
    public void putStage(String key, StageData data) {
        stageConfigMap.put(key, data);
    }

    /**
     * ステージデータの一覧(読み取り専用)
     */
    public Map<String, StageData> getStageConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(stageConfigMap);
    }

    /**
     * ステージIDの取得
     * 
     * @return ステージID
     */
    public int getStageId() {
        return stage.getStageId();
    }

    /**
     * ステージIDの設定
     * 
     * @param id ID
     */
    public void setStageId(int id) {
        stage.setStageId(id);
    }
}