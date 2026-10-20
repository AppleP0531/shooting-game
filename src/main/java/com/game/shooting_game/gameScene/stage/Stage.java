package com.game.shooting_game.gameScene.stage;

import java.awt.Graphics;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.beam.BlockSpec;
import com.game.shooting_game.gameScene.ship.EnemyAffix;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.types.Enemy;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.interfaces.GameObject;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public class Stage implements GameObject {
    private static final String component = "[Stage]";

    @Getter
    private int stageId;
    @Getter
    @Setter
    private Difficulty difficulty = Difficulty.NORMAL; // 選んだ難易度(ステージを選び直すとNORMALに戻る)
    @Getter
    private int waveId;
    @Getter
    @Setter
    private int startWave = 1; // 無限モードの開始ウェーブ(スキルで高くできる)
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
     * ステージIDの設定(難易度はNORMALに戻る。必要なら続けてsetDifficultyで選ぶ)
     */
    public void setStageId(int stageId) {
        this.stageId = stageId;
        this.difficulty = Difficulty.NORMAL;
    }

    /**
     * リセット
     */
    public void reset() {
        // 無限モードは、選んだ開始ウェーブから始める(それ以外は最初から)
        waveId = stageId == EndlessWaves.STAGE_ID ? Math.max(1, startWave) - 1 : 0;
    }

    @Override
    public void update(double dt) {
        // 敵とボスがいなくなった時敵を更新、テストステージでない場合はウェーブを進める
        if (SHIP_M.getEnemyList().isEmpty() && SHIP_M.getBossList().isEmpty()) {
            if (SM.getSceneName() != SceneType.TEST)
                waveId++;

            newWave(waveId);
        }
    }

    @Override
    public void draw(Graphics g) {

    }

    /**
     * StageConfigの敵情報から船を作成
     * 
     * @param enemy 敵情報
     * @return 船
     */
    private Ship createShip(EnemyConfig enemy) {
        Ship ship = createBaseShip(enemy);
        // ナイトメアでは、ボス以外の敵に、一定の確率でランダムなエフェクトが付く
        if (difficulty == Difficulty.NIGHTMARE && ship instanceof Enemy) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            ship.applyAffix(EnemyAffix.roll(rnd.nextDouble(), rnd.nextDouble()));
        }
        return ship;
    }

    /**
     * StageConfigの敵情報から、エフェクトのない船を作成
     */
    private Ship createBaseShip(EnemyConfig enemy) {
        // 経路が指定されている場合は経路を通る小敵
        if (enemy.getPath() != null && !enemy.getPath().isEmpty()) {
            return ShipFactory.createPathEnemy(
                    enemy.getEnemyId(),
                    enemy.getMode(),
                    enemy.getLevel(),
                    enemy.getPath(),
                    enemy.isMirror(),
                    enemy.getDelay(),
                    enemy.getSpeedScale());
        }

        Ship ship = ShipFactory.createShip(
                enemy.getOwner(),
                enemy.getEnemyId(),
                enemy.getMode(),
                enemy.getLevel(),
                toScreen(enemy.getSpawnPos()),
                toScreen(enemy.getStayPos()),
                enemy.getEntrance());

        // 入場開始までの待ち時間(順番に登場させる用)
        if (ship instanceof Enemy e && enemy.getDelay() > 0)
            e.setEntranceDelay(enemy.getDelay());
        return ship;
    }

    /**
     * 設定座標(画面中央基準)を画面座標に変換
     * 
     * @param pos 設定座標
     * @return 画面座標
     */
    private Vector2 toScreen(Vector2 pos) {
        double x = pos == null ? 0 : pos.getX();
        double y = pos == null ? 0 : pos.getY();
        return new Vector2(
                MAIN.getWidth() / 2 + x * ShootingGameMain.getScreenSizeAdj(),
                y * ShootingGameMain.getScreenSizeAdj());
    }

    /**
     * 新しいウェーブ
     * 
     * @param id ウェーブID
     */
    private void newWave(int id) {
        // 無限モード: ウェーブはその場で作る(終わりがなく、ゲームオーバーまで続く)
        if (stageId == EndlessWaves.STAGE_ID) {
            EndlessWaves.Wave wave = EndlessWaves.generate(id);
            BeamManager.getInstance().setGimmicks(wave.gimmicks());
            BeamManager.getInstance().setBlocks(wave.blocks());
            LogUtils.systemLog("[INFO]", component,
                    "The current wave is " + ANSI.YELLOW + "endless wave" + id + ANSI.RESET);
            for (EnemyConfig enemy : wave.enemies()) {
                try {
                    SHIP_M.addShip(createShip(enemy));
                } catch (Exception e) {
                    LogUtils.systemLog("[ERROR]", component, "Failed to spawn enemy '" + enemy.getEnemyId() + "'.");
                }
            }
            return;
        }

        Map<String, EnemyConfig[]> enemyList = STAGE_M.getStageConfig().get("stage" + stageId).getEnemyList();
        // 敵リストが存在するとき読み込む
        if (enemyList != null) {
            EnemyConfig[] waveData = enemyList.get("wave" + id);
            // 現在ウェーブの敵を読み込む、敵がいなくなったらリザルト画面へ
            if (waveData != null) {
                // このウェーブのビームギミックを設定(前のウェーブの予定は破棄される)
                Map<String, BeamSpec[]> gimmicks = STAGE_M.getStageConfig().get("stage" + stageId).getGimmicks();
                BeamManager.getInstance().setGimmicks(gimmicks == null ? null : gimmicks.get("wave" + id));
                Map<String, BlockSpec[]> blocks = STAGE_M.getStageConfig().get("stage" + stageId).getBlocks();
                BeamManager.getInstance().setBlocks(blocks == null ? null : blocks.get("wave" + id));

                // 現在ウェーブログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "The current wave is " + ANSI.YELLOW + "wave" + id + ANSI.RESET);

                for (EnemyConfig enemy : waveData) {
                    try {
                        SHIP_M.addShip(createShip(enemy));
                    } catch (Exception e) {
                        LogUtils.systemLog(
                                "[ERROR]",
                                component,
                                "Failed to spawn enemy '" + enemy.getEnemyId() + "''. \n" + "Skipping.");
                    }
                }
            } else {
                BeamManager.getInstance().setGimmicks(null);
                SHIP_M.getPlayer().animation("breakAway");
            }

        } else
            SM.changeScene(SceneType.NORMALSTAGE);
    }
}