package com.game.shooting_game.gameScene.bullet.barrage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.utils.Vector2;

/**
 * 照準点の予告(ANCHOR弾幕用)
 * 画面上の点に、縮んでいく照準の輪を出して「ここから弾が出る」と知らせ、予告時間が終わったら弾幕の発射処理を呼ぶ。
 */
public final class AnchorMarkers {
    private static AnchorMarkers instance;

    /** 予告1つ */
    private static final class Marker {
        final Vector2 pos;
        final double warnTime;
        final Runnable onFire;
        double elapsed = 0;

        Marker(Vector2 pos, double warnTime, Runnable onFire) {
            this.pos = pos;
            this.warnTime = Math.max(0.05, warnTime);
            this.onFire = onFire;
        }
    }

    private final List<Marker> markers = new ArrayList<>();

    private AnchorMarkers() {
        AnchorMarkers.instance = this;
    }

    public static AnchorMarkers getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, AnchorMarkers::new);
    }

    /**
     * 予告を足す
     * 
     * @param pos      照準する点(画面座標)
     * @param warnTime 予告の時間(秒)
     * @param onFire   予告が終わったときに呼ぶ処理(弾の発射)
     */
    public void add(Vector2 pos, double warnTime, Runnable onFire) {
        markers.add(new Marker(new Vector2(pos), warnTime, onFire));
    }

    /**
     * 進行(予告が終わったものは発射して消える)
     */
    public void update(double dt) {
        for (Marker m : new ArrayList<>(markers)) {
            m.elapsed += dt;
            if (m.elapsed >= m.warnTime) {
                markers.remove(m);
                m.onFire.run();
            }
        }
    }

    /**
     * 予告をすべて消す(ステージの開始・ボスのゲージ破壊など)
     */
    public void clear() {
        markers.clear();
    }

    /**
     * 照準の描画(輪が点へ縮み、終わりに近づくと点滅する)
     */
    public void draw(Graphics g) {
        if (markers.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        for (Marker m : new ArrayList<>(markers)) {
            double p = Math.min(1, m.elapsed / m.warnTime);
            double outer = (70 - 52 * p) * adj; // 大きい輪が縮んでいく
            double inner = 14 * adj;
            boolean blink = p > 0.75 && ((int) (m.elapsed * 14) % 2 == 0);
            int alpha = (int) ((90 + 150 * p) * (blink ? 0.4 : 1.0));
            Color c = new Color(255, 80, 90, Math.min(255, alpha));
            double x = m.pos.getX();
            double y = m.pos.getY();

            g2.setStroke(new BasicStroke((float) (2 * adj)));
            g2.setColor(c);
            g2.draw(new Ellipse2D.Double(x - outer, y - outer, outer * 2, outer * 2));
            g2.draw(new Ellipse2D.Double(x - inner, y - inner, inner * 2, inner * 2));
            double cross = outer + 8 * adj;
            g2.draw(new Line2D.Double(x - cross, y, x - inner, y));
            g2.draw(new Line2D.Double(x + inner, y, x + cross, y));
            g2.draw(new Line2D.Double(x, y - cross, x, y - inner));
            g2.draw(new Line2D.Double(x, y + inner, x, y + cross));
            g2.fill(new Ellipse2D.Double(x - 2 * adj, y - 2 * adj, 4 * adj, 4 * adj));
        }
        g2.dispose();
    }
}
