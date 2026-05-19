package com.game.shooting_game.gameScene.stage;

import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.interfaces.GameObject;

import java.awt.Graphics;
import java.util.Map;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.SceneManager.SceneType;

import lombok.Setter;
import lombok.Getter;

public class Stage implements GameObject {
    private static final String component = "[Stage]";

    @Setter
    @Getter
    private int stageId;
    private int waveId;
    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final ShipManager SHIP_M;
    private final StageManager STAGE_M;

    public Stage() {
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        STAGE_M = StageManager.getInstance();

        waveId = 0;
    }

    /**
     * リセット
     */
    public void reset() {
        waveId = 0;
    }

    @Override
    public void update(double dt) {
        // 敵がいなくなった時敵を更新、テストステージでない場合はウェーブを進める
        if (SHIP_M.getEnemyList().size() == 0) {
            if (SM.getSceneName() != SceneType.TEST)
                waveId++;

            newWave(waveId);
        }
    }

    @Override
    public void draw(Graphics g) {

    }

    /**
     * 新しいウェーブ
     * 
     * @param id ウェーブID
     */
    private void newWave(int id) {
        Map<String, EnemyConfig[]> enemyList = STAGE_M.getStageConfig().get("stage" + stageId).getEnemyList();
        // 敵リストが存在するとき読み込む
        if (enemyList != null) {
            EnemyConfig[] waveData = enemyList.get("wave" + id);
            // 現在ウェーブの敵を読み込む、敵がいなくなったらリザルト画面へ
            if (waveData != null) {
                // 現在ウェーブログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "The current wave is " + ANSI.YELLOW + "wave" + id + ANSI.RESET);

                for (EnemyConfig enemy : waveData) {
                    SHIP_M.addShip(ShipFactory.createShip(
                            enemy.getOwner(),
                            enemy.getEnemyId(),
                            enemy.getLevel(),
                            new Vector2(
                                    MAIN.getWidth() / 2
                                            + enemy.getSpawnPos().getX() * ShootingGameMain.getScreenSizeAdj(),
                                    enemy.getSpawnPos().getY() * ShootingGameMain.getScreenSizeAdj()),
                            new Vector2(
                                    MAIN.getWidth() / 2
                                            + enemy.getStayPos().getX() * ShootingGameMain.getScreenSizeAdj(),
                                    enemy.getStayPos().getY() * ShootingGameMain.getScreenSizeAdj())));
                }
            } else
                SHIP_M.getPlayer().animation("breakAway");

        } else
            SM.changeScene(SceneType.NORMALSTAGE);
    }
}