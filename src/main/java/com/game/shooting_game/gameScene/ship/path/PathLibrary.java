package com.game.shooting_game.gameScene.ship.path;

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

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * 小敵が通る経路の定義(PathConfig.json)の管理
 * 
 * 座標はStageConfigのspawnPosと同じ基準
 * x: 画面中央からのずれ, y: 画面上端からの距離(どちらもscreenSizeAdj倍される)
 */
public final class PathLibrary {
    private static final String component = "[PathLibrary]";
    private static final String CONFIG_PATH = "/static/configs/PathConfig.json";
    private static final TypeReference<Map<String, PathData>> TYPE_REF = new TypeReference<>() {
    };

    /**
     * 経路データ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class PathData {
        private List<Vector2> points = new ArrayList<>();
    }

    private static Map<String, PathData> pathMap = new HashMap<>();

    static {
        reload();
    }

    private PathLibrary() {
    }

    /**
     * config読み込み
     */
    public static void reload() {
        try (InputStream is = PathLibrary.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            pathMap = new ObjectMapper().readValue(is, TYPE_REF);

            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully (" + pathMap.size() + " paths)");
        } catch (IOException e) {
            pathMap = new HashMap<>();
            LogUtils.systemLog(
                    "[ERROR]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + " (" + e.getMessage()
                            + ")");
        }
    }

    /**
     * 経路の作成(画面座標に変換済み)
     * 
     * @param pathId 経路ID
     * @param mirror 左右反転するか
     * @return 経路
     */
    public static MovePath create(String pathId, boolean mirror) {
        return new MovePath(toScreen(getPoints(pathId), mirror));
    }

    /**
     * 経路の始点(画面座標)
     * 
     * @param pathId 経路ID
     * @param mirror 左右反転するか
     * @return 始点
     */
    public static Vector2 startPos(String pathId, boolean mirror) {
        return toScreen(getPoints(pathId), mirror).get(0);
    }

    /**
     * 通過点の取得(未定義の場合は画面中央を縦断する経路)
     */
    private static List<Vector2> getPoints(String pathId) {
        PathData data = pathId == null ? null : pathMap.get(pathId);
        if (data == null || data.getPoints() == null || data.getPoints().size() < 2) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "Path " + ANSI.YELLOW + pathId + ANSI.RESET + " is not defined. Using default path.");
            return List.of(new Vector2(0, -40), new Vector2(0, 780));
        }
        return data.getPoints();
    }

    /**
     * 設定座標を画面座標に変換
     */
    private static List<Vector2> toScreen(List<Vector2> points, boolean mirror) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double centerX = ShootingGameMain.getInstance().getWidth() / 2;
        double sign = mirror ? -1 : 1;

        List<Vector2> result = new ArrayList<>();
        for (Vector2 p : points)
            result.add(new Vector2(centerX + sign * p.getX() * adj, p.getY() * adj));
        return result;
    }
}
