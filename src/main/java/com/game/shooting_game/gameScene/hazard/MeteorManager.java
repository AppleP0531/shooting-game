package com.game.shooting_game.gameScene.hazard;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.game.shooting_game.gameScene.DamageCalculator;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * 隕石(ステージギミック)の管理
 * ウェーブごとに出す予定(MeteorSpec)を受け取り、時間になったら出す。自機の弾で壊れ、自機に当たるとダメージ。
 * 前のウェーブの隕石は消さず、そのまま落ちて出ていく。
 */
public final class MeteorManager implements Manager {
    private static MeteorManager instance;

    /** 出す予定(1つの MeteorSpec につき1つ) */
    private static final class Spawner {
        final MeteorSpec spec;
        double timer;
        int fired = 0;

        Spawner(MeteorSpec spec) {
            this.spec = spec;
            this.timer = spec.getDelay();
        }

        boolean done() {
            return spec.getInterval() <= 0 ? fired >= Math.max(1, spec.getCount())
                    : spec.getCount() > 0 && fired >= spec.getCount();
        }
    }

    private final List<Spawner> spawners = new ArrayList<>();
    private final List<Meteor> meteors = new ArrayList<>();

    private MeteorManager() {
        MeteorManager.instance = this;
    }

    public static MeteorManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, MeteorManager::new);
    }

    @Override
    public void setup() {
        spawners.clear();
        meteors.clear();
    }

    /**
     * ウェーブの隕石の予定を設定(前のウェーブの予定は破棄する。落ちている隕石は残る)
     */
    public void setMeteors(MeteorSpec[] specs) {
        spawners.clear();
        if (specs == null)
            return;
        for (MeteorSpec s : specs)
            spawners.add(new Spawner(s));
    }

    @Override
    public void update(double dt) {
        Player player = ShipManager.getInstance().getPlayer();
        boolean playing = player != null && "none".equals(player.getAnimationName());
        if (!playing)
            return;

        for (Spawner sp : new ArrayList<>(spawners)) {
            if (sp.done())
                continue;
            sp.timer -= dt;
            if (sp.timer <= 0) {
                meteors.add(new Meteor(sp.spec));
                sp.fired++;
                sp.timer += Math.max(0.1, sp.spec.getInterval());
            }
        }
        spawners.removeIf(Spawner::done);

        for (Meteor m : meteors)
            m.update(dt);
        collide(player);
        meteors.removeIf(m -> m.isDead() || m.isGone());
    }

    private void collide(Player player) {
        // 自機の弾でHPを削る(貫通弾は同じ隕石に2度当たらない)
        for (Bullet b : BulletManager.getInstance().getBulletList()) {
            if (b.getOwner() != ShipKinds.PLAYER || !b.isLife())
                continue;
            for (Meteor m : meteors) {
                if (m.isDead()
                        || GameUtils.distSq(b.getPos(), m.getPos()) > Math.pow(m.getRadius() + b.getCollisionSize(), 2))
                    continue;
                if (!b.registerHit(m))
                    continue;
                m.hit(b.getDamage() * DamageCalculator.bulletAttackRate());
                if (b.consumeHit()) {
                    b.destroySelf();
                    break;
                }
            }
        }
        // 自機との接触(当たった隕石は砕ける。色は関係なく当たる)
        Vector2 p = player.getPos();
        for (Meteor m : meteors) {
            if (m.isDead())
                continue;
            if (GameUtils.distSq(p, m.getPos()) <= Math.pow(m.getRadius() + player.getCollisionSize(), 2)) {
                player.addDamage(DamageCalculator.toPlayer(m.getDamage()));
                m.shatter();
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (Meteor m : new ArrayList<>(meteors))
            m.draw(g2);
    }
}
