package src;

import src.utils.ANSI;
import src.utils.LogUtils;
import src.bagScene.BagManager;
import src.shopScene.ShopManager;
import src.testScene.TestManager;
import src.gameScene.GameManager;
import src.interfaces.Manager;
import src.titleScene.TitleManager;
import src.lobbyScene.LobbyManager;
import src.settingScene.SettingManager;
import src.stageScene.SelectStageManager;
import src.stageScene.infinity.InfinityStageManager;
import src.stageScene.normal.NormalStageManager;
import src.upgradeScene.UpgradeManager;
import src.gameResultScene.GameResultManager;

import java.awt.Graphics;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Collections;
import lombok.Getter;

/**
 * シーン管理クラス
 */
public final class SceneManager implements Manager {
    private static SceneManager instance;
    private static final String component = "[SceneManager] ";

    // シーンの種類
    public static enum SceneType {
        TITLE,
        LOBBY,
        BAG,
        UPGRADE,
        SHOP,
        SELECTSTAGE,
        NORMALSTAGE,
        INFINITYSTAGE,
        SETTING,
        GAME,
        GAMERESULT,
        TEST
    }

    private final Map<SceneType, Manager> scenes = new EnumMap<>(SceneType.class);
    @Getter
    private List<SceneType> previousScenes = new ArrayList<>();
    @Getter
    private SceneType sceneName;

    // コンストラクタ
    private SceneManager() {
        SceneManager.instance = this;
        scenes.put(SceneType.TITLE, TitleManager.getInstance());
        scenes.put(SceneType.LOBBY, LobbyManager.getInstance());
        scenes.put(SceneType.BAG, BagManager.getInstance());
        scenes.put(SceneType.UPGRADE, UpgradeManager.getInstance());
        scenes.put(SceneType.SHOP, ShopManager.getInstance());
        scenes.put(SceneType.SELECTSTAGE, SelectStageManager.getInstance());
        scenes.put(SceneType.NORMALSTAGE, NormalStageManager.getInstance());
        scenes.put(SceneType.INFINITYSTAGE, InfinityStageManager.getInstance());
        scenes.put(SceneType.SETTING, SettingManager.getInstance());
        scenes.put(SceneType.GAME, GameManager.getInstance());
        scenes.put(SceneType.GAMERESULT, GameResultManager.getInstance());
        scenes.put(SceneType.TEST, TestManager.getInstance());
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static SceneManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, SceneManager::new);
    }

    @Override
    public void setup() {
        // シーンロード開始ログ
        LogUtils.systemLog(
                "[DEBUG]",
                component,
                "loading Scene...");

        // マネージャーのセットアップ
        Manager current = getCurrentManager();
        if (current != null)
            current.setup();

        // シーンロード完了ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.YELLOW + sceneName + " Scene" + ANSI.RESET + " loaded.");
    }

    @Override
    public void update(double dt) {
        // マネージャーの更新
        Manager current = getCurrentManager();
        if (current != null)
            current.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        // マネージャーの描画
        Manager current = getCurrentManager();
        if (current != null)
            current.draw(g);
    }

    /**
     * シーンの切り替え
     * 
     * @param target 切替先シーン名
     */
    public void changeScene(SceneType target) {
        // 遷移履歴に追加
        previousScenes.add(sceneName);
        sceneName = target;
        setup();
    }

    /**
     * ひとつ前のシーンに戻る
     */
    public void changeToPreviousScene() {
        SceneType previousScene = previousScenes.get(previousScenes.size() - 1);
        previousScenes.remove(previousScenes.size() - 1);

        sceneName = previousScene;
        setup();
    }

    /**
     * 現在のシーンで実行するマネージャーの取得
     * 
     * @return マネージャー
     */
    private Manager getCurrentManager() {
        return (sceneName == null) ? null : scenes.get(sceneName);
    }

    /**
     * シーンのマップの取得
     * 
     * @return シーンマップ
     */
    public Map<SceneType, Manager> getSceneMap() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(scenes);
    }
}