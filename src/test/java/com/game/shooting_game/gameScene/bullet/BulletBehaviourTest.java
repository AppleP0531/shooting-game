package com.game.shooting_game.gameScene.bullet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.bullet.types.Accel;
import com.game.shooting_game.gameScene.bullet.types.Redirect;
import com.game.shooting_game.gameScene.bullet.types.Redirect.Phase;
import com.game.shooting_game.gameScene.bullet.types.Split;

class BulletBehaviourTest {
    @Test
    @DisplayName("Accel: 初速から最終速度まで直線的に加速し、その後は一定")
    void accelSpeed() {
        assertEquals(30, Accel.speedAt(100, 0.3, 1.8, 1.5, 0), 1e-9);
        assertEquals(105, Accel.speedAt(100, 0.3, 1.8, 1.5, 0.75), 1e-9);
        assertEquals(180, Accel.speedAt(100, 0.3, 1.8, 1.5, 1.5), 1e-9);
        assertEquals(180, Accel.speedAt(100, 0.3, 1.8, 1.5, 9), 1e-9);
        assertEquals(180, Accel.speedAt(100, 0.3, 1.8, 0, 0), 1e-9);
    }

    @Test
    @DisplayName("Redirect: 直進 → 停止 → 発射の順に段階が進む")
    void redirectPhase() {
        assertEquals(Phase.OUT, Redirect.phaseAt(10, 100, 0, 0.5));
        assertEquals(Phase.PAUSE, Redirect.phaseAt(100, 100, 0.2, 0.5));
        assertEquals(Phase.GO, Redirect.phaseAt(100, 100, 0.5, 0.5));
        assertEquals(Phase.GO, Redirect.phaseAt(100, 100, 0, 0));
    }

    @Test
    @DisplayName("Redirect: 向き直り後は倍率の速度まで加速する")
    void redirectGoSpeed() {
        assertEquals(100, Redirect.goSpeed(100, 1.6, 0, 0.4), 1e-9);
        assertEquals(130, Redirect.goSpeed(100, 1.6, 0.2, 0.4), 1e-9);
        assertEquals(160, Redirect.goSpeed(100, 1.6, 1, 0.4), 1e-9);
        double a = Redirect.blinkAlpha(0.3);
        assertTrue(a >= 0.5 && a <= 0.9);
    }

    @Test
    @DisplayName("Split: 距離指定があれば距離で、なければ時間で分裂")
    void splitTrigger() {
        assertTrue(Split.shouldSplit(1.0, 0.95, 0, 0));
        assertFalse(Split.shouldSplit(0.5, 0.95, 0, 0));
        assertFalse(Split.shouldSplit(5.0, 0.95, 100, 200));
        assertTrue(Split.shouldSplit(0.1, 0.95, 200, 200));
    }

    @Test
    @DisplayName("Split: FAN は親の向きを中心に扇、nullやFANは従来どおり")
    void splitFan() {
        double[] a = Split.childAngles("FAN", 3, Math.toRadians(80), 1.0, 2.0);
        assertEquals(1.0 - Math.toRadians(40), a[0], 1e-9);
        assertEquals(1.0, a[1], 1e-9);
        assertEquals(1.0 + Math.toRadians(40), a[2], 1e-9);
        assertEquals(1.0, Split.childAngles(null, 1, 1, 1.0, 2.0)[0], 1e-9);
    }

    @Test
    @DisplayName("Split: RING は親の向きから等分、AIM は狙い角を中心にする")
    void splitRingAim() {
        double[] r = Split.childAngles("RING", 4, 0, 0.5, 0);
        assertEquals(4, r.length);
        assertEquals(0.5, r[0], 1e-9);
        assertEquals(0.5 + Math.PI / 2, r[1], 1e-9);
        assertEquals(0.5 + Math.PI * 1.5, r[3], 1e-9);

        double[] m = Split.childAngles("AIM", 3, Math.toRadians(60), 0, 2.0);
        assertEquals(2.0, m[1], 1e-9);
        assertEquals(2.0 - Math.toRadians(30), m[0], 1e-9);
        // 狙う相手がいない(NaN)ときは親の向き
        assertEquals(0.3, Split.childAngles("AIM", 1, 0, 0.3, Double.NaN)[0], 1e-9);
    }
}
