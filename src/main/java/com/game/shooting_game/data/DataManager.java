package com.game.shooting_game.data;

import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.entity.UserEntity;
import com.game.shooting_game.entity.UserShipEntity;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.repository.UserRepository;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

@Component
public final class DataManager implements Manager {
    private static final String component = "[DataManager]";
    private static ConfigurableApplicationContext context;
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
        private String username = "";
        private String lastPlayTime;
        private String totalPlayTime = "00:00:00";
        private String selectedShip = "Ship1";
        private String subShip;
        private Progress progress = new Progress();
        private Resources resources = new Resources();
        private Map<String, ShipStats> ships = new LinkedHashMap<>();

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
            private Map<ResourceKinds, Integer> equipments = new HashMap<>(); // 資源
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
            if (!ships.containsKey(shipId)) {
                ships.put(shipId, new ShipStats());
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        shipId + " unlocked.");
            }
        }
    }

    // レベル情報
    public record UserLevelResult(int level, int exp) {
    }

    private static final TypeReference<List<Integer>> TYPE_REF = new TypeReference<>() {
    };

    private static LocalDateTime sessionStartTime;

    private DataManager() {
        DataManager.instance = this;
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
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
        data.setUsername(entity.getUsername());
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
        data.getResources().setEquipments(entity.getEquipments());

        // 船の情報のコピー
        entity.getOwnedShips().forEach(shipEntity -> {
            String shipId = shipEntity.getShipId();
            data.unlockShip(shipId);

            // アップグレード情報のコピー
            if (shipEntity.getUpgrades() != null) {
                data.getShips().get(shipId).getUpgrades().putAll(shipEntity.getUpgrades());
            }
        });

        return data;
    }

    /**
     * UserDataからUserEntityへの変換と同期
     * 
     * @param entity
     */
    public static void syncUserDataToEntity(UserEntity entity) {
        // 基本フィールド
        entity.setLastPlayTime(userData.getLastPlayTime());
        entity.setTotalPlayTime(userData.getTotalPlayTime());
        entity.setSelectedShip(userData.getSelectedShip());
        entity.setSubShip(userData.getSubShip());

        // 進捗
        entity.setExp(userData.getProgress().getExp());
        entity.setClearedStage(userData.getProgress().getClearedStage());
        entity.getHighScores().clear();
        if (userData.getProgress().getHighScores() != null) {
            entity.getHighScores().putAll(userData.getProgress().getHighScores());
        }

        // 資源
        entity.setCoins(userData.getResources().getCoins());
        entity.getEquipments().clear();
        if (userData.getResources().getEquipments() != null) {
            entity.getEquipments().putAll(userData.getResources().getEquipments());
        }

        // 船の情報
        entity.getOwnedShips().clear();
        userData.getShips().forEach((shipId, stats) -> {
            UserShipEntity shipEntity = new UserShipEntity();
            shipEntity.setShipId(shipId);

            if (stats.getUpgrades() != null) {
                shipEntity.getUpgrades().putAll(stats.getUpgrades());
            }
            entity.getOwnedShips().add(shipEntity);
        });
    }

    /**
     * データ読み込み
     */
    public static void load(String username, String mode) {
        // ロード開始ログ
        LogUtils.systemLog(
                "[DEBUG]",
                component,
                "Attempting to load user data...");

        sessionStartTime = LocalDateTime.now();

        // セーブデータが存在した場合は読み込む、存在しなければ新規作成
        if (mode.equals("login")) {
            UserEntity entity = userRepository.findByUsername(username).orElse(null);
            userData = convertToUserData(entity);
        } else if (mode.equals("create")) {
            create(username);
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
            // 総プレイ時間の更新
            saveSessionPlayTime();

            // 時間を保存
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            userData.setLastPlayTime(now.format(formatter));

            // データベースのエンティティを取得（存在しない場合は新規作成）
            UserEntity entity = userRepository.findByUsername(userData.getUsername()).orElse(null);

            // データの同期
            syncUserDataToEntity(entity);

            // データベースに保存
            userRepository.save(entity);

            // セーブ成功ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    "User data saved successfully.");
        } catch (Exception e) {
            // セーブ失敗ログ
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Failed to persist user data.");
        }
    }

    /**
     * データ新規作成(初期化)
     */
    public static void create(String username) {
        // 新規作成開始ログ
        LogUtils.systemLog(
                "[DEBUG]",
                component,
                "Initializing a new profile...");

        userData = new UserData();
        userData.setUsername(username);
    }

    /**
     * パスワードの認証
     * 
     * @param username
     * @param password
     * @return 認証成功したかどうか
     */
    public static boolean authenticate(String username, String password) {
        UserService userService = context.getBean(UserService.class);
        return userService.authenticate(username, password);
    }

    /**
     * パスワードの変更
     * 
     * @param username
     * @param password
     * @return 変更成功したかどうか
     */
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        UserService userService = context.getBean(UserService.class);
        return userService.changePassword(username, oldPassword, newPassword);
    }

    /**
     * パスワードの強制変更
     * 
     * @param username
     * @param password
     * @return 変更成功したかどうか
     */
    public static boolean forceChangePassword(String username, String newPassword) {
        UserService userService = context.getBean(UserService.class);
        return userService.forceChangePassword(username, newPassword);
    }

    /**
     * ユーザーの削除
     * 
     * @param username
     * @param password
     * @return 削除成功したかどうか
     */
    public static boolean delete(String username, String password) {
        UserService userService = context.getBean(UserService.class);
        return userService.deleteUser(username, password);
    }

    /**
     * ユーザーの強制削除
     * 
     * @param username
     * @return 削除成功したかどうか
     */
    public static boolean forceDelete(String username) {
        UserService userServise = context.getBean(UserService.class);
        return userServise.forceDeleteUser(username);
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
        Duration sessionDuration = Duration.between(sessionStartTime,
                LocalDateTime.now());
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

    @Autowired
    public static void setContext(ConfigurableApplicationContext context) {
        DataManager.context = context;
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