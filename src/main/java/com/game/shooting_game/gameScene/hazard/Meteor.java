package com.game.shooting_game.gameScene.hazard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.concurrent.ThreadLocalRandom;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.utils.Vector2;

import lombok.Getter;

/**
 * 落ちてくる隕石(ステージギミック)
 * 自機に当たるとダメージ、自機の弾でHPを削って壊せる。画面の下へ出たら消える。
 */
public final class Meteor {
    @Getter
    private final Vector2 pos;
    private final double vx;
    private final double vy;
    @Getter
    private final double radius; // 画面倍率込み
    @Getter
    private final double damage; // 自機に当たったときのダメージ(防御などの計算前)
    private final double spinSpeed; // 見た目の回転(ラジアン/秒)
    private final double maxHp;
    private double hp;
    private double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
    private boolean dead = false;

    public Meteor(MeteorSpec spec) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        ShootingGameMain main = ShootingGameMain.getInstance();
        double half = main.getWidth() / adj / 2;
        double x = spec.getX() != null ? spec.getX()
                : ThreadLocalRandom.current().nextDouble(-half + spec.getRadius(), half - spec.getRadius());
        this.pos = new Vector2(main.getWidth() / 2.0 + x * adj, spec.getY() * adj);
        this.vx = spec.getVx() * adj;
        this.vy = spec.getVy() * adj;
        this.radius = spec.getRadius() * adj;
        this.damage = spec.getDamage();
        this.maxHp = Math.max(1, spec.getHp() * StageModifiers.enemyHpRate());
        this.hp = maxHp;
        this.spinSpeed = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
    }

    public void update(double dt) {
        pos.setX(pos.getX() + vx * dt);
        pos.setY(pos.getY() + vy * dt);
        angle += spinSpeed * dt;
    }

    /** ダメージを受ける。壊れたらtrue */
    public boolean hit(double amount) {
        hp -= amount;
        if (hp <= 0)
            dead = true;
        return dead;
    }

    /** 自機に当たって砕けた */
    public void shatter() {
        dead = true;
    }

    public boolean isDead() {
        return dead;
    }

    /** もう画面に戻ってこないか */
    public boolean isGone() {
        ShootingGameMain main = ShootingGameMain.getInstance();
        return pos.getY() - radius > main.getHeight() || pos.getX() < -radius * 2 - 200
                || pos.getX() > main.getWidth() + radius * 2 + 200;
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();
        // ごつごつした多角形(回転する)
        int n = 9;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            double a = angle + Math.PI * 2 * i / n;
            double r = radius * (0.82 + 0.18 * Math.sin(i * 2.3 + 1.1));
            xs[i] = (int) (pos.getX() + Math.cos(a) * r);
            ys[i] = (int) (pos.getY() + Math.sin(a) * r);
        }
        float damaged = (float) (1 - hp / maxHp); // 傷むほど赤みが増す
        g2.setColor(new Color(90 + (int) (80 * damaged), 84 - (int) (30 * damaged), 80 - (int) (30 * damaged)));
        g2.fillPolygon(xs, ys, n);
        g2.setColor(new Color(170, 160, 150));
        g2.setStroke(new BasicStroke(Math.max(1.5f, (float) (2 * adj))));
        g2.drawPolygon(xs, ys, n);
        // クレーター
        g2.setColor(new Color(60, 56, 54));
        int c = (int) (radius * 0.35);
        g2.fillOval((int) (pos.getX() + Math.cos(angle + 0.6) * radius * 0.35) - c / 2,
                (int) (pos.getY() + Math.sin(angle + 0.6) * radius * 0.35) - c / 2, c, c);
    }
}
