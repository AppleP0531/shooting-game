package com.game.shooting_game.gameScene.beam;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * ビーム1本
 * 予兆(WARN)→ 照射(ACTIVE)→ 消滅(FADE)の順に進む。ダメージが出るのは照射中だけ。
 */
public class Beam {
    public static enum Team {
        PLAYER, // 自機・サブ機のビーム(敵に当たる)
        HOSTILE // ステージ・敵・ボスのビーム(自機に当たる)
    }

    public static enum State {
        WARN, ACTIVE, FADE, DEAD
    }

    private static final Color BLUE = new Color(70, 150, 255);
    private static final Color RED = new Color(255, 70, 85);
    private static final double LENGTH = 3000; // 十分長い長さ(画面外まで)
    private static final double RAMP_IN = 0.12; // 照射開始から太くなりきるまでの時間
    private static final double LETHAL_RATE = 0.8; // 見た目の太さに対する当たり判定の太さ(見た目より少し甘くする)

    @Getter
    private final Team team;
    @Getter
    private ColorMode mode;
    @Getter
    private State state = State.WARN;
    @Getter
    private final double damage; // 敵のビーム: 1回のダメージ / 自機のビーム: 1秒あたりのダメージ
    @Getter
    private final double bossRate;
    @Getter
    @Setter
    private double damageScale = 1.0; // 自機のビームのダメージ倍率(スキル・サブ機パーツ)

    private final Vector2 origin;
    private double angle; // 基準角(ラジアン)
    private final double sweep; // 照射中に振る角度(ラジアン)
    private final double warnTime;
    private final double activeTime;
    private final double fadeTime;
    private final double width; // 太さ(画面倍率込み)
    private final double lockFraction;
    private double timer = 0;

    @Setter
    private Supplier<ColorMode> modeSupplier; // 色を撃った機体に追従させる
    @Setter
    private Supplier<Vector2> originSupplier; // 発射位置を撃った機体に追従させる
    @Setter
    private boolean trackPlayerX; // 予兆の間、自機のXを追う
    @Setter
    private boolean trackPlayerAim; // 予兆の間、自機の方向を向く
    @Setter
    private boolean lengthToTop; // 上端までの長さにする(自機のビーム)

    public Beam(Team team, BeamSpec spec, ColorMode mode, Vector2 origin, double angle) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        this.team = team;
        this.mode = mode;
        this.origin = new Vector2(origin);
        this.angle = angle;
        this.sweep = Math.toRadians(spec.getSweep());
        this.warnTime = Math.max(0.01, spec.getWarn());
        this.activeTime = Math.max(0.05, spec.getDuration());
        this.fadeTime = Math.max(0.01, spec.getFade());
        this.width = spec.getWidth() * adj;
        this.damage = spec.getDamage();
        this.bossRate = spec.getBossRate();
        this.lockFraction = Math.clamp(spec.getLock(), 0, 1);
    }

    public void update(double dt) {
        timer += dt;

        if (modeSupplier != null)
            mode = modeSupplier.get();
        if (originSupplier != null) {
            Vector2 o = originSupplier.get();
            origin.setX(o.getX());
            origin.setY(o.getY());
        }

        switch (state) {
            case WARN -> {
                // 予兆の序盤は自機を追い、終盤は狙いを固定する(動いてかわせる猶予)
                Player player = ShipManager.getInstance().getPlayer();
                if (player != null && timer < warnTime * lockFraction) {
                    if (trackPlayerX)
                        origin.setX(player.getPos().getX());
                    if (trackPlayerAim)
                        angle = GameUtils.targetVec(origin, player.getPos());
                }
                if (timer >= warnTime) {
                    state = State.ACTIVE;
                    timer = 0;
                }
            }
            case ACTIVE -> {
                if (timer >= activeTime) {
                    state = State.FADE;
                    timer = 0;
                }
            }
            case FADE -> {
                if (timer >= fadeTime)
                    state = State.DEAD;
            }
            default -> {
            }
        }
    }

    /**
     * 今の角度(照射中は振る)
     */
    private double currentAngle() {
        return switch (state) {
            case WARN -> angle - sweep / 2;
            case ACTIVE -> angle + sweep * (timer / activeTime - 0.5);
            default -> angle + sweep / 2;
        };
    }

    /**
     * 太さの伸び具合(0~1)
     */
    private double growth() {
        return switch (state) {
            case ACTIVE -> Math.min(1, timer / RAMP_IN);
            case FADE -> Math.max(0, 1 - timer / fadeTime);
            default -> 0;
        };
    }

    private double length() {
        return lengthToTop ? Math.max(0, origin.getY() + 40) : LENGTH * ShootingGameMain.getScreenSizeAdj();
    }

    /**
     * 当たり判定の半分の太さ
     */
    private double lethalHalfWidth() {
        return width * 0.5 * LETHAL_RATE * growth();
    }

    /**
     * 点からビームの線分までの最短距離と、線分上の最寄りの点
     */
    private Vector2 nearestPoint(Vector2 p) {
        double a = currentAngle();
        double dx = Math.cos(a);
        double dy = Math.sin(a);
        double t = (p.getX() - origin.getX()) * dx + (p.getY() - origin.getY()) * dy;
        t = Math.clamp(t, 0, length());
        return new Vector2(origin.getX() + dx * t, origin.getY() + dy * t);
    }

    /**
     * 円(自機の当たり判定)に当たっているか
     */
    public boolean hitsCircle(Vector2 center, double radius) {
        if (state != State.ACTIVE)
            return false;
        Vector2 n = nearestPoint(center);
        double dx = n.getX() - center.getX();
        double dy = n.getY() - center.getY();
        double r = lethalHalfWidth() + radius;
        return dx * dx + dy * dy <= r * r;
    }

    /**
     * 敵の船に当たっているか(見た目に沿った判定)
     */
    public boolean hitsShip(Ship ship) {
        if (state != State.ACTIVE)
            return false;
        Vector2 n = nearestPoint(ship.getPos());
        double half = lethalHalfWidth();
        // 中心を通っているときは確実に当てる
        double dx = n.getX() - ship.getPos().getX();
        double dy = n.getY() - ship.getPos().getY();
        if (dx * dx + dy * dy <= half * half * 0.25)
            return true;
        return ship.collidesWithCircle(n, half);
    }

    public boolean isDead() {
        return state == State.DEAD;
    }

    private static Color base(ColorMode mode) {
        return mode == ColorMode.RED ? RED : BLUE;
    }

    private static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.clamp(a * 255, 0, 255));
    }

    /**
     * 太さhの帯(中心が明るく、縁が透明になるグラデーション)を長さlen分描く
     */
    private static void drawBand(Graphics2D g2, double len, double h, Color center, double centerAlpha) {
        if (h <= 0.5)
            return;
        float[] fractions = { 0f, 0.5f, 1f };
        Color[] colors = { alpha(center, 0), alpha(center, centerAlpha), alpha(center, 0) };
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Double(0, -h / 2), new Point2D.Double(0, h / 2), fractions, colors));
        g2.fill(new Rectangle2D.Double(0, -h / 2, len, h));
    }

    public void draw(Graphics2D g2) {
        if (state == State.DEAD)
            return;

        double adj = ShootingGameMain.getScreenSizeAdj();
        AffineTransform oldTransform = g2.getTransform();
        var oldComposite = g2.getComposite();
        var oldPaint = g2.getPaint();
        var oldStroke = g2.getStroke();
        UIBuilder.applyHighQualityRenderSettings(g2);

        Color col = base(mode);
        double len = length();

        g2.translate(origin.getX(), origin.getY());
        g2.rotate(currentAngle());

        if (state == State.WARN) {
            // 予兆: 細い線の点滅と、危険範囲のうっすらした帯、発射位置のチャージ光
            double pulse = 0.5 + 0.5 * Math.sin(timer * 18);
            boolean hostile = team == Team.HOSTILE;
            if (hostile)
                drawBand(g2, len, width * LETHAL_RATE, col, 0.12 + 0.10 * (timer / warnTime));
            g2.setStroke(new BasicStroke((float) (2 * adj)));
            g2.setColor(alpha(col, 0.25 + 0.4 * pulse));
            g2.drawLine(0, 0, (int) len, 0);

            double charge = Math.min(1, timer / warnTime);
            double r = width * (0.25 + 0.55 * charge);
            flare(g2, 0, 0, r, col, 0.4 + 0.5 * charge);
        } else {
            double g = growth();
            double flicker = 1 + 0.05 * Math.sin(timer * 70);
            double w = width * g * flicker;

            drawBand(g2, len, w * 1.7, col, 0.30); // 外側の光
            drawBand(g2, len, w, col, 0.95); // 本体
            drawBand(g2, len, w * 0.34, Color.WHITE, 0.95); // 芯
            flare(g2, 0, 0, width * 1.0 * g, col, 0.9); // 発射位置の光
        }

        g2.setTransform(oldTransform);
        g2.setComposite(oldComposite);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    /**
     * 発射位置の丸い光
     */
    private static void flare(Graphics2D g2, double cx, double cy, double r, Color col, double a) {
        if (r < 1)
            return;
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) r,
                new float[] { 0f, 0.35f, 1f },
                new Color[] { alpha(Color.WHITE, a), alpha(col, a * 0.8), alpha(col, 0) }));
        g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }
}
