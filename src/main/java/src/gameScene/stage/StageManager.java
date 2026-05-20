package src.gameScene.stage;

import src.ResourceManager.ResourceKinds;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.gameScene.ship.ShipManager.ShipKinds;
import src.interfaces.Manager;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class StageManager implements Manager {
    private static final String component = "[StageManager]";
    private static StageManager instance;

    // ----- データクラスの定義 -----
    /**
     * ステージ情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class StageData {
        private Map<ResourceKinds, String> rewardList;
        private Map<String, EnemyConfig[]> enemyList;

        /**
         * 獲得リソース量の決定
         * 
         * @param resNameKey 獲得リソース名
         * @return 獲得量
         */
        public int getRandomResourceAmount(ResourceKinds resNameKey) {
            String range = rewardList.get(resNameKey);

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
        private ShipKinds owner;
        private String enemyId;
        private int level;
        private Vector2 spawnPos;
        private Vector2 stayPos;

        public void setEnemyId(String enemyId) {
            this.enemyId = enemyId;

            if (enemyId.startsWith("Enemy"))
                this.owner = ShipKinds.ENEMY;
            if (enemyId.startsWith("Boss"))
                this.owner = ShipKinds.BOSS;
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
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            stageConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET);
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
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