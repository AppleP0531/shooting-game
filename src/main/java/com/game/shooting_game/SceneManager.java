package com.game.shooting_game;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.encyclopediaScene.EncyclopediaManager;
import com.game.shooting_game.gameResultScene.GameResultManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.lobbyScene.LobbyManager;
import com.game.shooting_game.settingScene.SettingManager;
import com.game.shooting_game.gachaScene.GachaManager;
import com.game.shooting_game.shopScene.ShopManager;
import com.game.shooting_game.stageScene.SelectStageManager;
import com.game.shooting_game.stageScene.extra.ExtraStageManager;
import com.game.shooting_game.stageScene.infinity.InfinityStageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.testScene.TestManager;
import com.game.shooting_game.titleScene.TitleManager;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.ui.CoinHud;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.animation.AnimationManager;

public final class SceneManager implements Manager {
    private static SceneManager instance;
    private static final String component = "[SceneManager] ";

    AnimationManager animation; // アニメーションマネージャー

    // シーンの種類
    public static enum SceneType {
        TITLE,
        LOBBY,
        BAG,
        UPGRADE,
        SHOP,
        GACHA,
        ENCYCLOPEDIA,
        SELECTSTAGE,
        NORMALSTAGE,
        INFINITYSTAGE,
        EXTRA_MODULE,
        EXTRA_BOSS,
        EXTRA_MATERIAL,
        EXTRA_SPECIAL,
        SETTING,
        GAME,
        GAMERESULT,
        TEST
    }

    private final Map<SceneType, Manager> scenes = new EnumMap<>(SceneType.class);
    @Getter
    private List<SceneType> prevScenes = new ArrayList<>();
    @Getter
    private SceneType sceneName;

    private SceneManager() {
        SceneManager.instance = this;
        animation = AnimationManager.getInstance();

        scenes.put(SceneType.TITLE, TitleManager.getInstance());
        scenes.put(SceneType.LOBBY, LobbyManager.getInstance());
        scenes.put(SceneType.BAG, BagManager.getInstance());
        scenes.put(SceneType.UPGRADE, UpgradeManager.getInstance());
        scenes.put(SceneType.SHOP, ShopManager.getInstance());
        scenes.put(SceneType.GACHA, GachaManager.getInstance());
        scenes.put(SceneType.ENCYCLOPEDIA, EncyclopediaManager.getInstance());
        scenes.put(SceneType.SELECTSTAGE, SelectStageManager.getInstance());
        scenes.put(SceneType.NORMALSTAGE, NormalStageManager.getInstance());
        scenes.put(SceneType.INFINITYSTAGE, InfinityStageManager.getInstance());
        for (String[] c : ExtraStageManager.CATEGORIES)
            scenes.put(SceneType.valueOf("EXTRA_" + c[0]), new ExtraStageManager(c[0], c[1]));
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
        // アニメーションの更新
        animation.update(dt);

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

        // 所持コイン(コインを使う画面の左上)
        CoinHud.draw(g, sceneName);
    }

    /**
     * シーンの切り替え
     * 
     * @param target 切替先シーン名
     */
    public void changeScene(SceneType target) {
        // 遷移履歴に追加
        prevScenes.add(sceneName);
        sceneName = target;
        setup();
    }

    /**
     * 前のシーンに戻る
     */
    public void changeToPrevScene() {
        SceneType prevScene = prevScenes.get(prevScenes.size() - 1);
        prevScenes.remove(prevScenes.size() - 1);
        sceneName = prevScene;

        // ゲームシーンの場合はもう一つ戻る
        if (sceneName == SceneType.GAME)
            changeToPrevScene();

        setup();
    }

    /**
     * 現在のシーンで実行するマネージャーの取得
     * 
     * @return マネージャー
     */
    public Manager getCurrentManager() {
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