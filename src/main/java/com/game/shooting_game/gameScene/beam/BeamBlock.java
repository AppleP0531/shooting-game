package com.game.shooting_game.gameScene.beam;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.utils.Vector2;

import lombok.Getter;

/**
 * ビームを撃つ四角いブロック(ステージギミック)
 * 位置は少しずつ画面の下へ流れ、画面から外れたら消える。
 */
public final class BeamBlock {
    private static final Color BLUE = new Color(70, 150, 255);
    private static final Color RED = new Color(255, 70, 85);

    private final BlockSpec spec;
    private final BeamSpec beamSpec;
    private final BeamEmitter emitter;
    private final List<double[]> dirs = new ArrayList<>(); // 各方向の単位ベクトル(dx, dy)
    @Getter
    private final Vector2 pos;
    private final double half; // 一辺の半分(画面倍率込み)
    private int cycle = 0;
    private double warnLeft = 0; // 次の発射までの予兆の光らせ用
    private ColorMode nextMode = null;

    public BeamBlock(BlockSpec spec) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        ShootingGameMain main = ShootingGameMain.getInstance();
        this.spec = spec;
        this.pos = new Vector2(main.getWidth() / 2.0 + spec.getX() * adj, spec.getY() * adj);
        this.half = spec.getSize() * adj / 2;

        this.beamSpec = new BeamSpec();
        beamSpec.setColor(spec.getColor());
        beamSpec.setWarn(spec.getWarn());
        beamSpec.setDuration(spec.getDuration());
        beamSpec.setWidth(spec.getWidth());
        beamSpec.setDamage(spec.getDamage());
        beamSpec.setDelay(spec.getDelay());
        beamSpec.setInterval(spec.getInterval());
        beamSpec.setCount(spec.getCount());
        this.emitter = new BeamEmitter(beamSpec);

        for (String d : spec.getDirs() == null ? new String[] { "DOWN" } : spec.getDirs()) {
            switch (d.toUpperCase()) {
                case "UP" -> dirs.add(new double[] { 0, -1 });
                case "LEFT" -> dirs.add(new double[] { -1, 0 });
                case "RIGHT" -> dirs.add(new double[] { 1, 0 });
                default -> dirs.add(new double[] { 0, 1 });
            }
        }
    }

    /**
     * 移動と発射
     * 
     * @param dt         経過時間
     * @param playerMode 自機の色
     * @param shooter    発射処理(方向ごとのビームの発射)
     */
    public void update(double dt, ColorMode playerMode, BeamManager shooter) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        pos.setX(pos.getX() + spec.getVx() * adj * dt);
        pos.setY(pos.getY() + spec.getVy() * adj * dt);

        if (!onScreen() || emitter.done())
            return;

        if (emitter.update(dt, 1.0)) {
            ColorMode mode = emitter.pickColor(playerMode);
            nextMode = mode;
            warnLeft = spec.getWarn() + spec.getDuration();
            if ("CYCLE".equalsIgnoreCase(spec.getPattern())) {
                shooter.fireFromBlock(this, beamSpec, mode, dirs.get(cycle % dirs.size()));
                cycle++;
            } else {
                for (double[] d : dirs)
                    shooter.fireFromBlock(this, beamSpec, mode, d);
            }
        }
        warnLeft = Math.max(0, warnLeft - dt);
    }

    /**
     * 画面の中に(少しでも)いるか
     */
    public boolean onScreen() {
        ShootingGameMain main = ShootingGameMain.getInstance();
        return pos.getX() > -half && pos.getX() < main.getWidth() + half && pos.getY() > -half
                && pos.getY() < main.getHeight() + half;
    }

    /**
     * もう画面に戻ってこないか(消してよいか)
     */
    public boolean isGone() {
        ShootingGameMain main = ShootingGameMain.getInstance();
        double adj = ShootingGameMain.getScreenSizeAdj();
        boolean below = pos.getY() > main.getHeight() + half && spec.getVy() >= 0;
        boolean left = pos.getX() < -half && spec.getVx() <= 0;
        boolean right = pos.getX() > main.getWidth() + half && spec.getVx() >= 0 && pos.getX() > main.getWidth() + 400 * adj;
        return below || left || right;
    }

    public void draw(Graphics2D g2) {
        if (!onScreen())
            return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();
        int cx = (int) pos.getX();
        int cy = (int) pos.getY();
        int s = (int) (half * 2);
        boolean alt = !"BLUE".equalsIgnoreCase(spec.getColor()) && !"RED".equalsIgnoreCase(spec.getColor());
        Color main = "RED".equalsIgnoreCase(spec.getColor()) ? RED : BLUE;
        if (alt && nextMode != null)
            main = nextMode == ColorMode.RED ? RED : BLUE;

        // ノズル(撃つ方向)
        g2.setColor(main.darker());
        int nz = Math.max(3, (int) (6 * adj));
        for (double[] d : dirs) {
            int nx = cx + (int) (d[0] * half) - (d[0] != 0 ? nz / 2 : nz);
            int ny = cy + (int) (d[1] * half) - (d[1] != 0 ? nz / 2 : nz);
            int nw = d[0] != 0 ? nz : nz * 2;
            int nh = d[1] != 0 ? nz : nz * 2;
            g2.fillRect(nx + (d[0] > 0 ? nz / 2 : 0), ny + (d[1] > 0 ? nz / 2 : 0), nw, nh);
        }

        // 本体
        g2.setColor(new Color(20, 22, 34));
        g2.fillRect(cx - s / 2, cy - s / 2, s, s);
        Color frame = main;
        if (alt && warnLeft <= 0) { // 次の色が決まる前の待機中は青赤の二色で示す
            g2.setColor(RED);
            g2.fillRect(cx - s / 2, cy - s / 2, s / 2, 3);
            frame = BLUE;
        }
        g2.setColor(frame);
        g2.setStroke(new BasicStroke(Math.max(2f, (float) (2.5 * adj))));
        g2.drawRect(cx - s / 2, cy - s / 2, s, s);

        // 中心の光(撃つ直前〜照射中は明るく)
        int a = warnLeft > 0 ? 230 : 90;
        g2.setColor(new Color(main.getRed(), main.getGreen(), main.getBlue(), a));
        int c = s / 3;
        g2.fillRect(cx - c / 2, cy - c / 2, c, c);
    }
}
