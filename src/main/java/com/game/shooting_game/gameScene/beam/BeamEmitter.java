package com.game.shooting_game.gameScene.beam;

import java.util.concurrent.ThreadLocalRandom;

import com.game.shooting_game.gameScene.GameManager.ColorMode;

/**
 * ビームの発射タイミングの管理(遅延・間隔・回数・色の割り当て)
 */
public final class BeamEmitter {
    private final BeamSpec spec;
    private double remaining; // 次に撃つまでの時間
    private int fired = 0; // 撃った回数
    private boolean altFlag = false; // ALT用

    public BeamEmitter(BeamSpec spec) {
        this.spec = spec;
        this.remaining = Math.max(0, spec.getDelay());
    }

    public BeamSpec getSpec() {
        return spec;
    }

    /**
     * 時間を進める
     * 
     * @param dt            経過時間
     * @param intervalScale 発射間隔の倍率(小さいほど速く撃つ)
     * @return 今撃つならtrue
     */
    public boolean update(double dt, double intervalScale) {
        return update(dt, intervalScale, true);
    }

    /**
     * 時間を進める(撃つ条件つき)
     * 撃てる時間になっても条件を満たしていなければ撃たずに待ち、満たした瞬間に撃つ。
     * 自機のビームを「狙える敵がいるときだけ」撃たせて、画面が騒がしくならないようにするために使う。
     * 
     * @param dt            経過時間
     * @param intervalScale 発射間隔の倍率(小さいほど速く撃つ)
     * @param ready         今撃ってよいか
     * @return 今撃つならtrue
     */
    public boolean update(double dt, double intervalScale, boolean ready) {
        if (done())
            return false;

        remaining -= dt;
        if (remaining > 0)
            return false;
        if (!ready) {
            remaining = 0; // 条件が整うまで待つ(間隔のカウントは進めない)
            return false;
        }

        fired++;
        remaining = spec.getInterval() * intervalScale;
        return true;
    }

    /**
     * もう撃たないか
     */
    public boolean done() {
        if (fired == 0)
            return false;
        if (spec.getInterval() <= 0)
            return true;
        return spec.getCount() > 0 && fired >= spec.getCount();
    }

    /**
     * 今回の色を決める
     * 
     * @param playerMode 自機の今の色(PLAYER指定用)
     * @return 色
     */
    public ColorMode pickColor(ColorMode playerMode) {
        String c = spec.getColor() == null ? "ALT" : spec.getColor().toUpperCase();
        return switch (c) {
            case "RED" -> ColorMode.RED;
            case "BLUE" -> ColorMode.BLUE;
            case "RANDOM" -> ThreadLocalRandom.current().nextBoolean() ? ColorMode.RED : ColorMode.BLUE;
            case "PLAYER" -> playerMode == null ? ColorMode.BLUE : playerMode;
            default -> { // ALT
                altFlag = !altFlag;
                yield altFlag ? ColorMode.RED : ColorMode.BLUE;
            }
        };
    }
}
