package com.game.shooting_game.gameScene.ship;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.DamageCalculator;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.bullet.Bullet.Direction;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemFactory;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.item.ResourceFactory;
import com.game.shooting_game.gameScene.item.ResourceFactory.DropModel;
import com.game.shooting_game.gameScene.ship.path.PathLibrary;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ShipManager implements Manager {
    private static final String component = "[ShipManager]";
    private static ShipManager instance;

    @Setter
    @Getter
    private Player player;
    @Getter
    private List<Ship> enemyList = new ArrayList<>(); // 敵のリスト
    @Getter
    private List<Ship> bossList = new ArrayList<>(); // ボスのリスト

    // 弾の所有者
    public static enum ShipKinds {
        OBJECT,
        PLAYER,
        ENEMY,
        BOSS
    }

    // ----- データクラスの定義 -----
    /**
     * 機体全体情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ShipDefinition {
        private ShipConfig datas;
        private Map<String, ShotConfig[]> levels = new HashMap<>();
        private BeamSpec[] beams; // この機体が撃つビーム(敵: 到着後に撃つ / 自機: minLevel以上で撃つ)

        @JsonAnySetter
        public void addLevelData(String key, ShotConfig[] configs) {
            this.levels.put(key, configs);
        }

        public ShotConfig[] getLevelData(String levelKey) {
            return levels.get(levelKey);
        }
    }

    /**
     * 機体のステータス
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ShipConfig {
        private String imagePath;
        private int cost;
        private SheetConfig[] sheets;
        private double frameRate;
        private int imageSize;
        private double collisionSize;
        private double hp;
        private double speed;
        private double score;
        private String coins;
    }

    /**
     * スプライトシートの情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class SheetConfig {
        private Vector2 grids;
        private int useCount;
    }

    /**
     * 機体の発射する弾情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ShotConfig {
        private BulletKinds bulletKind;
        private int bulletId;
        private Vector2 pos;
        private String bulletVecRef;
        private int vecAdj;
        private Direction direction;
        private double cooldown; // この弾を撃った後の発射間隔(秒)。0なら弾設定のクールタイムを使う
    }

    private Map<ShipKinds, Map<String, ShipDefinition>> shipConfigMap; // 船データが格納された呼び出し用のマップ
    private Map<String, BufferedImage[]> shipImageCache; // 画像データ
    private static final TypeReference<Map<ShipKinds, Map<String, ShipDefinition>>> TYPE_REF = new TypeReference<>() {
    };

    private final ShootingGameMain MAIN;
    private final GameManager GM;
    private final ItemManager IM;

    private ShipManager() {
        ShipManager.instance = this;
        MAIN = ShootingGameMain.getInstance();
        GM = GameManager.getInstance();
        IM = ItemManager.getInstance();

        reload();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static ShipManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, ShipManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/ShipConfig.json";
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
            shipConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.error(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET + "\n"
                            +
                            ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    private void loadImages() {
        final String IMAGE_BASE_PATH = "/static/img/ship";

        shipImageCache = new HashMap<>();
        // ShipKindsの階層でループ
        for (Map.Entry<ShipKinds, Map<String, ShipDefinition>> ownerEntry : shipConfigMap.entrySet()) {
            ShipKinds owner = ownerEntry.getKey();
            Map<String, ShipDefinition> definitions = ownerEntry.getValue();

            // 機体IDの階層でループ
            for (Map.Entry<String, ShipDefinition> shipEntry : definitions.entrySet()) {
                String shipId = shipEntry.getKey();
                ShipConfig data = shipEntry.getValue().getDatas();

                if (data == null) {
                    // 船データ未定義ログ
                    LogUtils.systemLog(
                            "[WARN]",
                            component,
                            "data for ship " + ANSI.YELLOW + owner + ANSI.RESET + " is not defined.");
                    continue;
                }

                // ColorModeごとに処理
                for (ColorMode mode : ColorMode.values()) {
                    String imagePath = IMAGE_BASE_PATH + data.getImagePath();
                    try {
                        String suffix = "";
                        if (!shipId.equals("explosion") && owner != ShipKinds.PLAYER) {
                            suffix = "_" + mode.toString().toLowerCase();
                            imagePath = UIBuilder.createPathWithSuffix(imagePath, suffix);
                        }

                        BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                        SheetConfig[] sheets = data.getSheets();
                        Vector2 grids = new Vector2(1, 0);
                        int useCount = 0;
                        grids.setX(sheets[0].getGrids().getX());
                        for (SheetConfig sheet : sheets) {
                            grids.setY(grids.getY() + sheet.getGrids().getY());
                            useCount += sheet.getUseCount();
                        }

                        // 切り取り
                        BufferedImage[] cutSheet = UIBuilder.cutSpriteSheet(
                                masterSheet,
                                new Vector2(grids.getX(), grids.getY()),
                                (int) (grids.getX() * grids.getY()));

                        // 必要部分のみを抜き取りフレーム配列に保存
                        BufferedImage[] frames = new BufferedImage[useCount];
                        int addedCount = 0;
                        int adj = 0;
                        for (int i = 0; i < sheets.length; i++) {
                            useCount = sheets[i].getUseCount();

                            if (i > 0) {
                                addedCount += sheets[i - 1].getUseCount();
                                adj += (int) (sheets[i - 1].getGrids().getY()) * (int) (grids.getX());
                            }

                            for (int j = 0; j < useCount; j++) {
                                frames[addedCount + j] = cutSheet[adj + j];
                            }
                        }

                        // 画像をキャッシュに保存
                        shipImageCache.put(shipId + suffix, frames);
                    } catch (IOException e) {
                        switch (e.getMessage()) {
                            // 画像ファイル未発見ログ
                            case "FILE_NOT_FOUND" -> LogUtils.systemLog(
                                    "[WARN]",
                                    component,
                                    "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                            // 画像読み込みエラーログ
                            case "BROKEN_FILE" -> LogUtils.systemLog(
                                    "[ERROR]",
                                    component,
                                    "Failed to load image: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                            // その他エラーログ
                            default -> System.out.println(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setup() {
        clear("all");

        // プレイヤー生成
        player = new Player(
                DataManager.getUserData().getSelectedShip(),
                ColorMode.BLUE,
                new Vector2(MAIN.getWidth() / 2, MAIN.getHeight()));
    }

    @Override
    public void update(double dt) {
        // プレイヤーの更新
        if (player != null)
            player.update(dt);

        // 敵の更新
        updateAndClean(enemyList, dt);
        updateAndClean(bossList, dt);

        // ブロックは通り抜けられない(動いたあとにめり込んでいたら外へ押し出す。ボスは対象外)
        if (player != null)
            pushOutOfBlocks(player, true);
        for (Ship e : enemyList)
            pushOutOfBlocks(e, false);
    }

    /**
     * 船をブロックの外へ押し出す
     * 
     * @param ship       船
     * @param keepScreen 画面の外へ出ないようにするか(自機)
     */
    private void pushOutOfBlocks(Ship ship, boolean keepScreen) {
        double r = ship.bodyRadius();
        // 自機は画面の外へは押し出せない。端に押し付けられたら押し潰されてダメージを受ける
        java.awt.geom.Rectangle2D bounds = keepScreen
                ? new java.awt.geom.Rectangle2D.Double(r, r, MAIN.getWidth() - 2 * r, MAIN.getHeight() - 2 * r)
                : null;
        boolean crushed = BeamManager.getInstance().pushOutOfBlocks(ship, r, bounds);
        if (crushed && ship instanceof Player p)
            p.addDamage(DamageCalculator.ramToPlayer(p));
        if (keepScreen) {
            Vector2 p2 = ship.getPos();
            p2.setX(Math.max(r, Math.min(MAIN.getWidth() - r, p2.getX())));
            p2.setY(Math.max(r, Math.min(MAIN.getHeight() - r, p2.getY())));
        }
    }

    @Override
    public void draw(Graphics g) {
        player.draw(g);
        getAllEnemies().forEach(e -> e.draw(g));
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
        loadImages();
        BossLibrary.reload();
        PathLibrary.reload();
    }

    /**
     * 船の更新
     * 
     * @param ships 船
     * @param dt
     */
    private void updateAndClean(List<Ship> ships, double dt) {
        Iterator<Ship> iterator = ships.iterator();
        while (iterator.hasNext()) {
            Ship s = iterator.next();
            s.update(dt); // 更新

            // 除去要求(画面外へ離脱した敵など)はドロップ等なしで取り除く
            if (s.isRemoveRequested()) {
                iterator.remove();
                continue;
            }

            // 死亡処理
            if (s.isDead()) {
                // キル数カウントアップ
                GM.setKillCountSinceLastLevelUp(GM.getKillCountSinceLastLevelUp() + 1);
                if (s.isAllowDropItem()) {
                    dropItem(s); // アイテムドロップ
                    dropResource(s); // リソースドロップ
                }
                dropCoin(s); // コインドロップ
                s.addScore(); // スコア加算
                s.animation("death"); // 削除
                iterator.remove(); // リストから削除

                // ボス撃破時は残っている敵弾を消す
                if (s.getOwner() == ShipKinds.BOSS) {
                    BulletManager.getInstance().clearEnemyBullets();
                    BeamManager.getInstance().clearHostile();
                }
            }
        }
    }

    /**
     * アイテムドロップ
     * 
     * @param ship 参照する船
     */
    private void dropItem(Ship ship) {
        Item newItem;
        // アイテムを生成
        // 連続20体強化アイテムを落とさなかった場合は強制ドロップ
        // (スキルで必要撃破数を最大5減らせる)
        if (GM.getKillCountSinceLastLevelUp() >= 20 - (int) SkillService.getBonus("LEVELUP_DROP"))
            newItem = ItemFactory.createItem(new Vector2(ship.getPos()), ItemKinds.LEVEL_UP);
        else
            newItem = ItemFactory.createItem(new Vector2(ship.getPos()));

        // アイテムドロップ(nullはドロップ無し)
        if (newItem != null)
            IM.addItem(newItem);
    }

    /**
     * リソースドロップ
     * 
     * @param ship 参照する船
     */
    private void dropResource(Ship ship) {
        dropResource(new Vector2(ship.getPos()));
    }

    /**
     * リソースドロップ(位置指定)
     * 
     * @param pos 生成位置
     */
    private void dropResource(Vector2 pos) {
        dropResource(pos, DropModel.ENEMY_DEFEAT);
    }

    /**
     * リソースドロップ(位置・抽選モデル指定)
     * 
     * @param pos   生成位置
     * @param model 抽選モデル
     */
    private void dropResource(Vector2 pos, DropModel model) {
        Item newItem = ResourceFactory.createResource(model, pos);

        // 拾った時点でステージの獲得素材として記録される(Resource.effect)
        if (newItem != null)
            IM.addItem(newItem);
    }

    /**
     * ボスのゲージ破壊ボーナスドロップ
     * アイテムとリソースの抽選を指定回数回し、コインも落とす
     * 
     * @param boss  ボス
     * @param rolls 抽選回数
     */
    public void dropBossBonus(Ship boss, int rolls) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        rolls += (int) Math.floor(SkillService.getBonus("BOSS_ROLLS"));
        for (int i = 0; i < rolls; i++) {
            // 少しばらけた位置に落とす
            Vector2 pos = new Vector2(
                    boss.getPos().getX() + rnd.nextDouble(-50, 50) * adj,
                    boss.getPos().getY() + rnd.nextDouble(-20, 40) * adj);

            Item item = ItemFactory.createItem(new Vector2(pos));
            if (item != null)
                IM.addItem(item);

            dropResource(new Vector2(pos), DropModel.BOSS_BONUS);
        }

        // コインドロップ
        int coinAmount = boss.getDropCoinAmount();
        for (int i = 0; i < coinAmount; i++) {
            IM.addItem(ResourceFactory.createResource(new Vector2(boss.getPos()), ResourceKinds.COIN));
        }
    }

    /**
     * コインドロップ
     * 
     * @param ship 参照する船
     */
    private void dropCoin(Ship ship) {
        // ドロップするコイン量の決定
        int dropAmount = ship.getDropCoinAmount() * (ship.getShipLevel() + 1);

        for (int i = 0; i < dropAmount; i++) {
            IM.addItem(ResourceFactory.createResource(new Vector2(ship.getPos()), ResourceKinds.COIN));
        }
    }

    /**
     * 船をクリア
     * 
     * @param target クリア対象の船種(プレイヤー、敵、ボス)
     */
    public void clear(String target) {
        switch (target) {
            case "player" -> {
                player.kill(false);
            }
            case "enemy" -> {
                enemyList.clear();
            }
            case "boss" -> {
                bossList.clear();
            }
            case "all" -> {
                enemyList.clear();
                bossList.clear();
            }
        }
    }

    /**
     * 船を殺す
     * 
     * @param target 削除対象の船種(プレイヤー、敵、ボス)
     */
    public void kill(String target) {
        switch (target) {
            case "player" -> {
                if (player != null)
                    player.kill(false);
            }
            case "enemy" -> {
                if (!enemyList.isEmpty())
                    enemyList.forEach(s -> s.kill(true));
            }
            case "boss" -> {
                if (!bossList.isEmpty())
                    bossList.forEach(s -> s.kill(true));
            }
            case "all" -> {
                if (!enemyList.isEmpty())
                    enemyList.forEach(s -> s.kill(true));
                if (!bossList.isEmpty())
                    bossList.forEach(s -> s.kill(true));
            }
        }
    }

    /**
     * 敵の新規作成
     * 
     * @param ship 船
     */
    public void addShip(Ship ship) {
        switch (ship.getOwner()) {
            case ENEMY -> enemyList.add(ship);
            case BOSS -> bossList.add(ship);
            default -> throw new IllegalArgumentException("不明な機体の種類です: " + ship.getOwner());
        }
    }

    /**
     * プレイヤーのリロード
     * 
     * @param id
     */
    public void reloadPlayer(int id) {
        Vector2 pos = player.getPos();
        ColorMode mode = player.getMode();

        clear("player");
        player = new Player("Ship" + id, mode, pos);
    }

    /**
     * 発射されたプレイヤーの弾から最も近い敵を探す
     * 
     * @param pos 位置
     * @return 最も近い敵
     */
    public Ship getNearestTargetForPlayer(Vector2 pos) {
        double nearestDist = Double.POSITIVE_INFINITY;
        Ship nearestTarget = null;

        // 画面内で最も近くのEnemyまたはBossを探す
        for (Ship target : getAllEnemies()) {
            Vector2 targetPos = target.getPos();
            double distSq = GameUtils.distSq(targetPos, pos);

            if (target.inScreen() && distSq < nearestDist) {
                nearestDist = distSq;
                nearestTarget = target;
            }
        }
        return nearestTarget;
    }

    /**
     * 敵リストに指定の敵が存在するかチェック
     * 
     * @param target 探す対象の敵
     * @return いる場合true, いない場合false
     */
    public boolean findEnemy(Ship target) {
        return getAllEnemies().contains(target);
    }

    /**
     * 船データの取得
     * 
     * @return 船データ
     */
    public Map<ShipKinds, Map<String, ShipDefinition>> getShipConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(shipConfigMap);
    }

    /**
     * 画像データの取得
     * 
     * @return 画像データ
     */
    public Map<String, BufferedImage[]> getShipImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(shipImageCache);
    }

    /**
     * 敵、ボスリストの取得
     * 
     * @return 敵、ボスリスト
     */
    public List<Ship> getAllEnemies() {
        List<Ship> all = new ArrayList<>();
        all.addAll(enemyList);
        all.addAll(bossList);
        return all;
    }
}