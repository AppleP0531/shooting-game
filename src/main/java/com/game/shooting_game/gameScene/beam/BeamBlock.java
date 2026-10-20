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
 * 色と撃つ方向は出現したときに決まり、あとは変わらない。船は通り抜けられず、進行方向にいる船は押される。
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
    private final ColorMode mode; // 出現したときに決まる色(変わらない)
    private double warnLeft = 0; // 次の発射までの予兆の光らせ用

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
        // 色はBLUE/REDならそのまま。それ以外(ALTなど)は、出現時にどちらかに決める
        this.mode = switch (spec.getColor() == null ? "" : spec.getColor().toUpperCase()) {
            case "RED" -> ColorMode.RED;
            case "BLUE" -> ColorMode.BLUE;
            default -> java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? ColorMode.RED : ColorMode.BLUE;
        };

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
     * @param shooter    発射処理(方向ごとのビームの発射)
     */
    public void update(double dt, BeamManager shooter) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        pos.setX(pos.getX() + spec.getVx() * adj * dt);
        pos.setY(pos.getY() + spec.getVy() * adj * dt);

        if (!onScreen() || emitter.done())
            return;

        if (emitter.update(dt, 1.0)) {
            warnLeft = spec.getWarn() + spec.getDuration();
            for (double[] d : dirs)
                shooter.fireFromBlock(this, beamSpec, mode, d);
        }
        warnLeft = Math.max(0, warnLeft - dt);
    }

    /** 押し出しの結果 */
    public enum Push {
        NONE, // 重なっていない
        PUSHED, // 外へ押し出した
        SQUEEZED // 押し出す先が画面の外で、めり込んだまま(押し潰し)
    }

    private boolean crushing = false; // 押し潰しのダメージを与えている最中か(めり込みが解けるまで、続けて与えない)

    /**
     * 船がブロックに食い込んでいたら押し出す(船はブロックを通り抜けられない)
     * ブロックの進む向きに船がいるときは、その向きに押す。それ以外は、食い込みの浅い向きへ押し出す。
     * 押し出す先が画面の外になるとき(画面の端に押し付けられたとき)は、動かさずにSQUEEZEDを返す。
     * 
     * @param pos    船の中心(押し出した結果で書き換える)
     * @param radius 船の体の半径(画面倍率込み)
     * @param bounds 船が動ける範囲(nullなら制限なし)。中心がこの外へ出る押し出しは、押し潰しとして扱う
     */
    public Push pushOut(Vector2 pos, double radius, java.awt.geom.Rectangle2D bounds) {
        if (!onScreen())
            return Push.NONE;
        double adj = ShootingGameMain.getScreenSizeAdj();
        double dx = pos.getX() - this.pos.getX();
        double dy = pos.getY() - this.pos.getY();
        double ox = half + radius - Math.abs(dx);
        double oy = half + radius - Math.abs(dy);
        if (ox <= 0 || oy <= 0)
            return Push.NONE; // 重なっていない

        double vx = spec.getVx() * adj;
        double vy = spec.getVy() * adj;
        boolean pushAlongMove = false;
        boolean alongY = false;
        if (Math.abs(vy) >= Math.abs(vx) && vy != 0 && Math.signum(dy) == Math.signum(vy)) {
            pushAlongMove = true;
            alongY = true;
        } else if (Math.abs(vx) > Math.abs(vy) && Math.signum(dx) == Math.signum(vx)) {
            pushAlongMove = true;
        }

        double newX = pos.getX();
        double newY = pos.getY();
        if (pushAlongMove ? alongY : oy <= ox) {
            double sign = pushAlongMove ? Math.signum(vy) : (dy >= 0 ? 1 : -1);
            newY = this.pos.getY() + sign * (half + radius);
        } else {
            double sign = pushAlongMove ? Math.signum(vx) : (dx >= 0 ? 1 : -1);
            newX = this.pos.getX() + sign * (half + radius);
        }

        // 画面の端に押し付けられて、押し出せないとき: 横にずらさず、そのまま(押し潰し)
        if (bounds != null && !bounds.contains(newX, newY))
            return Push.SQUEEZED;

        pos.setX(newX);
        pos.setY(newY);
        return Push.PUSHED;
    }

    /**
     * 押し潰しのダメージを与えてよいか(めり込んでいる間は1回だけtrue)
     */
    public boolean beginCrush() {
        if (crushing)
            return false;
        crushing = true;
        return true;
    }

    /**
     * めり込みが解けた
     */
    public void endCrush() {
        crushing = false;
    }

    /**
     * 弾(円)がブロックに当たっているか
     */
    public boolean hits(Vector2 p, double radius) {
        if (!onScreen())
            return false;
        double cx = Math.max(pos.getX() - half, Math.min(pos.getX() + half, p.getX()));
        double cy = Math.max(pos.getY() - half, Math.min(pos.getY() + half, p.getY()));
        double dx = p.getX() - cx;
        double dy = p.getY() - cy;
        return dx * dx + dy * dy <= radius * radius;
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
        Color main = mode == ColorMode.RED ? RED : BLUE;

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
        g2.setColor(main);
        g2.setStroke(new BasicStroke(Math.max(2f, (float) (2.5 * adj))));
        g2.drawRect(cx - s / 2, cy - s / 2, s, s);

        // 中心の光(撃つ直前〜照射中は明るく)
        int a = warnLeft > 0 ? 230 : 90;
        g2.setColor(new Color(main.getRed(), main.getGreen(), main.getBlue(), a));
        int c = s / 3;
        g2.fillRect(cx - c / 2, cy - c / 2, c, c);
    }
}
