package com.game.shooting_game.gameScene.ship;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.bullet.barrage.BarrageSpec;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * ボスの戦闘定義(BossConfig.json)の管理
 * 
 * ボスのゲージ数 = gaugesの要素数。ゲージごとに動きと弾幕を定義する。
 * 見た目・当たり判定などはShipConfig.jsonのBOSS側に書く。
 */
public final class BossLibrary {
    private static final String component = "[BossLibrary]";
    private static final String CONFIG_PATH = "/static/configs/BossConfig.json";
    private static final TypeReference<Map<String, BossDefinition>> TYPE_REF = new TypeReference<>() {
    };

    /**
     * ボス1体分の定義
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class BossDefinition {
        private double gaugeHp = 1000; // 1ゲージあたりのHP
        private EntranceType entrance = EntranceType.SPIRAL; // 入場軌道
        private double entranceTime = 3.5; // 入場にかける時間(秒)
        private double transitionTime = 2.0; // ゲージ破壊後の移行時間(秒、この間は無敵)
        private List<GaugeDefinition> gauges = new ArrayList<>(); // ゲージごとの定義(先頭から順に削る)
        private int retreatAfterGauges = 0; // このゲージ数を壊されたら撤収する(0なら撤収しない)
        private double retreatTime = 0; // 戦闘開始からこの秒数が経つと撤収する(0なら撤収しない)
        private double coinStep = 0; // ダメージをこれだけ受けるたびにコインを落とす(0なら落とさない)
    }

    /**
     * ゲージ1本分の定義
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class GaugeDefinition {
        private String move = "SWAY"; // 動き(STATIC, SWAY, FIGURE8)
        private double moveAmpX = 60; // 横の振れ幅
        private double moveAmpY = 20; // 縦の振れ幅(FIGURE8)
        private double moveSpeed = 1.0; // 動きの速さ(ラジアン/秒)
        private int dropRolls = 5; // このゲージを破壊したときのドロップ抽選回数
        private List<BarrageSpec> barrages = new ArrayList<>(); // このゲージ中に同時に撃つ弾幕
        private List<BeamSpec> beams = new ArrayList<>(); // このゲージ中に撃つビーム
    }

    private static Map<String, BossDefinition> bossMap = new HashMap<>();

    static {
        reload();
    }

    private BossLibrary() {
    }

    /**
     * config読み込み
     */
    public static void reload() {
        try (InputStream is = BossLibrary.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            bossMap = new ObjectMapper().readValue(is, TYPE_REF);

            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully (" + bossMap.size() + " bosses)");
        } catch (IOException e) {
            bossMap = new HashMap<>();
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + " (" + e.getMessage()
                            + ")");
        }
    }

    /**
     * ボス定義の取得
     * 
     * @param bossId ボスID(ShipConfigのBOSSのキー)
     * @return 定義(未定義の場合は撃たない1ゲージのボス)
     */
    public static BossDefinition get(String bossId) {
        BossDefinition def = bossMap.get(bossId);
        if (def != null && def.getGauges() != null && !def.getGauges().isEmpty())
            return def;

        LogUtils.systemLog(
                "[WARN]",
                component,
                "Boss " + ANSI.YELLOW + bossId + ANSI.RESET + " is not defined in BossConfig.json. Using default.");

        BossDefinition fallback = new BossDefinition();
        fallback.getGauges().add(new GaugeDefinition());
        return fallback;
    }
}
