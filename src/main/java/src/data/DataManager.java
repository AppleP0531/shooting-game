package src.data;

import src.ResourceManager.ResourceKinds;
import src.entity.UserEntity;
import src.utils.ANSI;
import src.utils.LogUtils;
import src.utils.GameUtils;
import src.interfaces.Manager;
import src.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * データ管理クラス
 */
@Component
public final class DataManager implements Manager {
    private static final String component = "[DataManager]";
    private static DataManager instance;
    private static UserRepository userRepository;
    private static UserData userData;
    private static List<Integer> expData;

    // ----- データクラスの定義 -----
    /**
     * ユーザーデータ
     */
    @Data // Getter, Setterの自動生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    @JsonPropertyOrder({ "lastPlayTime", "totalPlayTime", "selectedShip", "subShip", "progress", "resources", "ships" }) // 並び順の指定
    public static class UserData {
        private String lastPlayTime;
        private String totalPlayTime = "00:00:00";
        private String selectedShip = "Ship1";
        private String subShip = "";
        private Progress progress = new Progress();
        private Resources resources = new Resources();
        private Map<String, ShipStats> ships = new LinkedHashMap<>();

        // コンストラクタ
        public UserData() {
            unlockShip("Ship1");
        }

        /**
         * 進捗
         */
        @Data // Getter, Setterの自動生成
        @NoArgsConstructor // 引数なしコンストラクタの生成
        @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
        public static class Progress {
            private int exp = 0; // 獲得経験値の総量
            private int clearedStage = 0; // ステージクリア状況
            private Map<String, Integer> highScores = new HashMap<>(); // ステージごとの最高スコア

            // 最高スコアの更新と保存
            public void updateHighScore(String stageKey, int score) {
                if (score > highScores.getOrDefault(stageKey, 0)) {
                    highScores.put(stageKey, score);
                }
            }

            // スコアの加算
            public void addExp(int amount) {
                exp += amount;
            }
        }

        /**
         * 所有資源
         */
        @Data // Getter, Setterの自動生成
        @NoArgsConstructor // 引数なしコンストラクタの生成
        @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
        public static class Resources {
            private int coins = 0; // コイン
            private Map<ResourceKinds, Integer> equipment = new HashMap<>(); // 資源
        }

        // 船の情報
        @Data // Getter, Setterの自動生成
        @NoArgsConstructor // 引数なしコンストラクタの生成
        @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
        public static class ShipStats {
            private Map<String, Integer> upgrades = new HashMap<>(); // 強化状況
        }

        // 新しい船のアンロック
        public void unlockShip(String shipId) {
            // まだ所有していない場合は追加
            if (!ships.containsKey(shipId))
                ships.put(shipId, new ShipStats());
        }
    }

    // レベル情報
    public record UserLevelResult(int level, int exp) {
    }

    private static final String SAVE_PATH = "saves/UserData.json";
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<Integer>> TYPE_REF = new TypeReference<>() {
    };

    private static LocalDateTime sessionStartTime;

    private DataManager() {
        DataManager.instance = this;
    }

    public static DataManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, DataManager::new);
    }

    /**
     * UserRepositoryのセット
     * 
     * @param repo
     */
    @Autowired
    public void setUserRepository(UserRepository repo) {
        DataManager.userRepository = repo;
    }

    /**
     * UserEntityからUserDataへの変換
     * 
     * @param entity UserEntityオブジェクト
     * @return
     */
    public static UserData convertToUserData(UserEntity entity) {
        UserData data = new UserData();

        // 基本情報のコピー
        data.setLastPlayTime(entity.getLastPlayTime());
        data.setTotalPlayTime(entity.getTotalPlayTime());
        data.setSelectedShip(entity.getSelectedShip());
        data.setSubShip(entity.getSubShip());

        // 進捗のコピー
        data.getProgress().setExp(entity.getExp());
        data.getProgress().setClearedStage(entity.getClearedStage());
        data.getProgress().setHighScores(entity.getHighScores());

        // 資源のコピー
        data.getResources().setCoins(entity.getCoins());
        data.getResources().setEquipment(entity.getEquipment());

        // 船の情報のコピー
        entity.getShipUpgrades().forEach((key, level) -> {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                String shipId = parts[0];
                String upgradeName = parts[1];

                // 船がまだ存在しない場合は追加
                data.unlockShip(shipId);

                // 強化状況の更新
                data.getShips().get(shipId).getUpgrades().put(upgradeName, level);
            }
        });

        return data;
    }

    public static void syncUserDataToEntity(UserEntity entity) {
        // 基本フィールド
        entity.setLastPlayTime(userData.getLastPlayTime());
        entity.setTotalPlayTime(userData.getTotalPlayTime());
        entity.setSelectedShip(userData.getSelectedShip());
        entity.setSubShip(userData.getSubShip());

        // 進捗
        entity.setExp(userData.getProgress().getExp());
        entity.setClearedStage(userData.getProgress().getClearedStage());
        entity.setHighScores(userData.getProgress().getHighScores());

        // 資源
        entity.setCoins(userData.getResources().getCoins());
        entity.setEquipment(userData.getResources().getEquipment());

        // 船の情報
        Map<String, Integer> shipUpgrades = new HashMap<>();
        userData.getShips().forEach((shipId, stats) -> {
            stats.getUpgrades().forEach((upgradeName, level) -> {
                String key = shipId + ":" + upgradeName;
                shipUpgrades.put(key, level);
            });
        });
        entity.setShipUpgrades(shipUpgrades);

    }

    /**
     * データ読み込み
     */
    public static void load() {

        // ロード開始ログ
        LogUtils.systemLog(
                "[DEBUG]",
                component,
                "Attempting to load user data from " + ANSI.YELLOW + SAVE_PATH + ANSI.RESET + ".");

        sessionStartTime = LocalDateTime.now();

        UserEntity entity = userRepository.findById(1L).orElse(null);

        // セーブデータが存在した場合は読み込む、存在しなければ新規作成
        if (entity != null) {
            userData = convertToUserData(entity);
        } else {
            // 移行用
            File file = new File(SAVE_PATH);
            if (file.exists()) {
                try {
                    userData = mapper.readValue(file, UserData.class);
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Existing userData file detected. Attempting migration to database.");
                } catch (IOException e) {
                    LogUtils.systemLog(
                            "[ERROR]",
                            component,
                            "Failed to load existing userData file during migration.", e);
                }
            } else {
                create();
            }
        }
        loadExpConfig();
        printWelcomeMessage();
    }

    /**
     * データ保存
     */
    public static synchronized void save() {
        if (userData == null || userRepository == null) {
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Cannot save user data: UserData or UserRepository is not initialized.");
            return;
        }

        try {
            // 時間を保存
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            userData.setLastPlayTime(now.format(formatter));

            // データベースのエンティティを取得（存在しない場合は新規作成）
            UserEntity entity = userRepository.findById(1L).orElseGet(() -> {
                UserEntity newEntity = new UserEntity();
                return newEntity;
            });

            // データの同期
            syncUserDataToEntity(entity);

            // データベースに保存
            userRepository.save(entity);

            // セーブ成功ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    "Game state successfully synchronized to " + ANSI.YELLOW + SAVE_PATH + ANSI.RESET + ".");
        } catch (Exception e) {
            // セーブ失敗ログ
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Failed to persist user data.");
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Please check write permissions at " + ANSI.YELLOW + SAVE_PATH + ANSI.RESET + ".", e);
        }
    }

    /**
     * データ新規作成(初期化)
     */
    public static void create() {
        try {
            // 新規作成開始ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Initializing a new profile...");

            File file = new File(SAVE_PATH);
            userData = new UserData();

            // フォルダがなければ作成
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                // フォルダ未発見ログ
                LogUtils.systemLog(
                        "[WARN]",
                        component,
                        ANSI.YELLOW + parentDir.getPath() + ANSI.RESET + " directory not found.");

                // フォルダ新規作成
                if (parentDir.mkdirs()) {
                    // 新規作成成功ログ
                    LogUtils.systemLog(
                            "[DEBUG]",
                            component,
                            "Created missing directory " + ANSI.YELLOW + parentDir.getPath() + ANSI.RESET + ".");
                }
            }

            // データ作成成功ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    "New userData successfully created at " + ANSI.YELLOW + SAVE_PATH + ANSI.RESET + ".");
        } catch (Exception e) {
            // 新規作成失敗ログ
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Failed to initialize userData at " + ANSI.YELLOW + SAVE_PATH + ANSI.RESET + ".", e);
        }
    }

    /**
     * 経験値テーブルの読み込み
     */
    private static void loadExpConfig() {
        final String CONFIG_PATH = "static/configs/Exp.json";
        ObjectMapper mapper = new ObjectMapper();

        try (
                InputStream is = DataManager.class.getClassLoader().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            expData = mapper.readValue(is, TYPE_REF);

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

    /**
     * ウェルカムメッセージ
     */
    private static void printWelcomeMessage() {
        String lastPlayTimeStr = userData.getLastPlayTime();

        // 初回プレイ時
        if (lastPlayTimeStr == null || lastPlayTimeStr.isEmpty()) {
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.YELLOW + "Welcome to your first flight!" + ANSI.RESET);
            return;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
            LocalDateTime lastTime = LocalDateTime.parse(lastPlayTimeStr, formatter);
            LocalDateTime now = LocalDateTime.now();

            // 2つの時刻の差分計算
            Duration duration = Duration.between(lastTime, now);

            long days = duration.toDays();
            long hours = duration.toHoursPart();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    String.format(
                            "Welcome back! It has been"
                                    + ANSI.YELLOW + " %d days %02d:%02d:%02d" + ANSI.RESET
                                    + " since your last session.",
                            days, hours, minutes, seconds));
        } catch (Exception e) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Could not calculate elapsed time.");
        }
    }

    /**
     * このセッションのプレイ時間を加算
     */
    public static void saveSessionPlayTime() {
        Duration sessionDuration = Duration.between(sessionStartTime, LocalDateTime.now());
        long sessionSeconds = sessionDuration.toSeconds();

        // 時間の合算
        String totalPlayTime = userData.getTotalPlayTime();
        long totalSeconds = GameUtils.parseTimeToSeconds(totalPlayTime);
        long newTotalSeconds = totalSeconds + sessionSeconds;
        userData.setTotalPlayTime(GameUtils.formatSecondsToTime(newTotalSeconds));

        sessionStartTime = LocalDateTime.now();
    }

    /**
     * ユーザーデータの取得
     * 
     * @return ユーザーデータ
     */
    public static UserData getUserData() {
        return userData;
    }

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {

    }

    public static UserLevelResult getUserLevel() {
        int exp = DataManager.getUserData().getProgress().getExp();
        int level = 0;
        int surplus = 0;
        while (level < expData.size()) {
            if (exp < expData.get(level)) {
                break;
            }

            exp -= expData.get(level);
            surplus = exp;
            level++;
        }

        return new UserLevelResult(level, surplus);
    }

    /**
     * 経験値データの取得
     * 
     * @return 経験値データ
     */
    public static List<Integer> getExpData() {
        // コピーの取得
        return new ArrayList<>(expData);
    }
}