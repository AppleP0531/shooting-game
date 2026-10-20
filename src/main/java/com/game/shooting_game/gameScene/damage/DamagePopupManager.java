package com.game.shooting_game.gameScene.damage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.GameSettings;

/**
 * ダメージ数字の表示
 * 船がダメージを受けたとき、機体の上に数字を出し、上へ浮かびながら薄く消える。
 * 累計にはせず、1回の被弾ごとに1つの数字を出す。会心は大きく橙色で「!」付き。
 * 設定画面の「ダメージ表記」がOFFのときは出さない。
 */
public final class DamagePopupManager implements Manager {
    private static final double LIFETIME = 0.9; // 数字が出ている時間(秒)
    private static final double FADE_START = 0.5; // 薄くなり始める時間(秒)
    private static final int MAX_POPUPS = 60; // 同時に出す数字の上限

    private static DamagePopupManager instance;

    private final List<Popup> popups = new ArrayList<>();

    /** 数字1つ */
    private static final class Popup {
        final double x;
        final double y;
        final ShipKinds owner;
        double amount;
        final boolean crit;
        double age = 0;

        Popup(double x, double y, ShipKinds owner, double amount, boolean crit) {
            this.crit = crit;
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.amount = amount;
        }
    }

    private DamagePopupManager() {
        DamagePopupManager.instance = this;
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static DamagePopupManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, DamagePopupManager::new);
    }

    /**
     * ダメージを受けたことを知らせる
     * 
     * @param target 受けた船
     * @param amount ダメージ量
     */
    public void add(Ship target, double amount, boolean crit) {
        if (amount <= 0 || !GameSettings.isDamageTextEnabled())
            return;

        // 累計にせず、1回の被弾ごとに数字を出す
        double adj = ShootingGameMain.getScreenSizeAdj();
        // 機体のまわりに、同じ船に出ている数字とできるだけ離れた位置へ出す(重なって読めなくならないように)
        double bx = target.getPos().getX();
        double by = target.getPos().getY() - 14 * adj;
        double x = bx, y = by;
        double bestDist = -1;
        for (int i = 0; i < 8; i++) {
            double cx = bx + (Math.random() - 0.5) * 56 * adj;
            double cy = by + (Math.random() - 0.5) * 30 * adj;
            // 近くに出ている数字までの最短距離(離れているほど良い)
            double nearest = Double.MAX_VALUE;
            for (Popup p : popups) {
                if (p.age > 0.6)
                    continue;
                double py = p.y - 28 * adj * (1 - Math.pow(1 - Math.min(1, p.age / LIFETIME), 2));
                nearest = Math.min(nearest, Math.hypot(cx - p.x, cy - py));
            }
            if (nearest > bestDist) {
                bestDist = nearest;
                x = cx;
                y = cy;
            }
        }
        popups.add(new Popup(x, y, target.getOwner(), amount, crit));
        if (popups.size() > MAX_POPUPS)
            popups.removeFirst();
    }

    @Override
    public void setup() {
        popups.clear();
    }

    @Override
    public void update(double dt) {
        for (Popup p : popups)
            p.age += dt;
        popups.removeIf(p -> p.age >= LIFETIME);
    }

    @Override
    public void draw(Graphics g) {
        if (popups.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        for (Popup p : new ArrayList<>(popups)) {
            // 上へ浮かぶ(はじめは速く、だんだん遅く)
            double t = Math.min(1, p.age / LIFETIME);
            double rise = 28 * adj * (1 - Math.pow(1 - t, 2));
            float alpha = (float) (p.age <= FADE_START ? 1 : Math.max(0, 1 - (p.age - FADE_START) / (LIFETIME - FADE_START)));

            boolean toPlayer = p.owner == ShipKinds.PLAYER;
            boolean toBoss = p.owner == ShipKinds.BOSS;
            double size = (toBoss ? 16 : toPlayer ? 15 : 12) * adj * (p.crit ? 1.5 : 1);
            Color color = p.crit ? new Color(255, 150, 30)
                    : toPlayer ? new Color(255, 100, 100) : toBoss ? new Color(255, 235, 130) : Color.WHITE;

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) size));
            FontMetrics fm = g2.getFontMetrics();
            String text = Math.max(1, Math.round(p.amount)) + (p.crit ? "!" : "");
            int x = (int) (p.x - fm.stringWidth(text) / 2.0);
            int y = (int) (p.y - rise);

            // 縁取り(背景に埋もれないように黒い影を付ける)
            g2.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
            g2.drawString(text, x + 1, y + 1);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * alpha)));
            g2.drawString(text, x, y);
        }
        g2.dispose();
    }
}
