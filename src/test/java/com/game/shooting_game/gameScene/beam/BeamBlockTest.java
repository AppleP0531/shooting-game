package com.game.shooting_game.gameScene.beam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.objenesis.ObjenesisStd;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.utils.Vector2;

/**
 * ShootingGameMain はコンストラクタが画面情報を触るため、Objenesis でコンストラクタを通さずに
 * 差し込む。幅・高さは 0 なので、ブロックを原点 (x=0, y=0) に置けば画面内として扱われる。
 * screenSizeAdj は既定値 1。
 */
class BeamBlockTest {
    @BeforeAll
    static void installMain() throws Exception {
        Field f = ShootingGameMain.class.getDeclaredField("instance");
        f.setAccessible(true);
        if (f.get(null) == null)
            f.set(null, new ObjenesisStd().newInstance(ShootingGameMain.class));
    }

    /** 原点に置いた一辺 size のブロック(angle は度) */
    private BeamBlock block(double size, double angleDeg, double vx, double vy) {
        BlockSpec s = new BlockSpec();
        s.setX(0);
        s.setY(0);
        s.setSize(size);
        s.setAngle(angleDeg);
        s.setVx(vx);
        s.setVy(vy);
        return new BeamBlock(s);
    }

    @Test
    @DisplayName("角度0: 内側と辺ぎりぎりの円は当たり、離れていれば当たらない")
    void hitsAxisAligned() {
        BeamBlock b = block(100, 0, 0, 0); // half = 50
        assertThat(b.hits(new Vector2(0, 0), 1)).isTrue();
        assertThat(b.hits(new Vector2(55, 0), 6)).isTrue();
        assertThat(b.hits(new Vector2(60, 0), 10)).isTrue(); // 境界(<=)
        assertThat(b.hits(new Vector2(70, 0), 5)).isFalse();
        // 角の外側: 距離は sqrt(2)*10 = 約14.1
        assertThat(b.hits(new Vector2(60, 60), 14)).isFalse();
        assertThat(b.hits(new Vector2(60, 60), 15)).isTrue();
    }

    @Test
    @DisplayName("45度回転: 角が菱形の頂点方向に伸びる")
    void hitsRotated45() {
        BeamBlock b = block(100, 45, 0, 0);
        double tip = 50 * Math.sqrt(2); // 頂点までの距離 約70.7
        assertThat(b.hits(new Vector2(tip - 1, 0), 0.5)).isTrue();
        assertThat(b.hits(new Vector2(tip + 5, 0), 1)).isFalse();
        // 回転前の角(50,50)は菱形の外側
        assertThat(b.hits(new Vector2(50, 50), 1)).isFalse();
        assertThat(b.hits(new Vector2(40, 0), 1)).isTrue();
    }

    @Test
    @DisplayName("重なっていなければ NONE で位置は変わらない")
    void pushNone() {
        BeamBlock b = block(100, 0, 0, 0);
        Vector2 p = new Vector2(200, 0);
        assertThat(b.pushOut(p, 10, null)).isEqualTo(BeamBlock.Push.NONE);
        assertThat(p.getX()).isEqualTo(200);
        assertThat(p.getY()).isZero();
    }

    @Test
    @DisplayName("角度0・静止: 食い込みの浅い面から外へ押し出す")
    void pushAxisAligned() {
        BeamBlock b = block(100, 0, 0, 0);
        Vector2 p = new Vector2(55, 5); // 右面に5食い込み(半径10: 50+10=60)
        assertThat(b.pushOut(p, 10, null)).isEqualTo(BeamBlock.Push.PUSHED);
        assertThat(p.getX()).isCloseTo(60, within(1e-9));
        assertThat(p.getY()).isCloseTo(5, within(1e-9));

        Vector2 q = new Vector2(-3, -52); // 上面から押し出す
        assertThat(b.pushOut(q, 10, null)).isEqualTo(BeamBlock.Push.PUSHED);
        assertThat(q.getY()).isCloseTo(-60, within(1e-9));
        assertThat(q.getX()).isCloseTo(-3, within(1e-9));
    }

    @Test
    @DisplayName("90度回転: 正方形なので角度0と同じ結果になる")
    void pushRotated90() {
        BeamBlock b = block(100, 90, 0, 0);
        Vector2 p = new Vector2(55, 5);
        assertThat(b.pushOut(p, 10, null)).isEqualTo(BeamBlock.Push.PUSHED);
        assertThat(p.getX()).isCloseTo(60, within(1e-6));
        assertThat(p.getY()).isCloseTo(5, within(1e-6));
    }

    @Test
    @DisplayName("45度回転: 菱形の辺の法線方向(斜め)へ押し出される")
    void pushRotated45() {
        BeamBlock b = block(100, 45, 0, 0);
        // 辺の法線は (1,1)/√2。中心から法線方向 55 の点(辺まで50, 半径10 → 5食い込み)
        double d = 55 / Math.sqrt(2);
        Vector2 p = new Vector2(d, d);
        assertThat(b.pushOut(p, 10, null)).isEqualTo(BeamBlock.Push.PUSHED);
        double expected = 60 / Math.sqrt(2);
        assertThat(p.getX()).isCloseTo(expected, within(1e-6));
        assertThat(p.getY()).isCloseTo(expected, within(1e-6));
        // 押し出し後より外側なら重なりなし
        assertThat(b.pushOut(new Vector2(p.getX() + 1, p.getY() + 1), 10, null)).isEqualTo(BeamBlock.Push.NONE);
    }

    @Test
    @DisplayName("ブロックの進行方向にいる船は進行方向へ押される")
    void pushAlongMotion() {
        BeamBlock b = block(100, 0, 0, 20); // 下へ進む
        // 横寄りに深く食い込んでいても、下側にいれば下へ押す
        Vector2 p = new Vector2(45, 30);
        assertThat(b.pushOut(p, 10, null)).isEqualTo(BeamBlock.Push.PUSHED);
        assertThat(p.getY()).isCloseTo(60, within(1e-9));
        assertThat(p.getX()).isCloseTo(45, within(1e-9));
    }

    @Test
    @DisplayName("押し出し先が bounds の外なら SQUEEZED で位置は変わらない")
    void squeezed() {
        BeamBlock b = block(100, 0, 0, 20);
        Rectangle2D bounds = new Rectangle2D.Double(-200, -200, 400, 100); // y は -100 まで
        Vector2 p = new Vector2(0, 30); // 下へ押すと y=60 で bounds 外
        assertThat(b.pushOut(p, 10, bounds)).isEqualTo(BeamBlock.Push.SQUEEZED);
        assertThat(p.getX()).isZero();
        assertThat(p.getY()).isEqualTo(30);
        // bounds 内に収まるなら通常通り押し出す
        Rectangle2D wide = new Rectangle2D.Double(-200, -200, 400, 400);
        assertThat(b.pushOut(p, 10, wide)).isEqualTo(BeamBlock.Push.PUSHED);
    }

    @Test
    @DisplayName("beginCrush はめり込み中1回だけ true、endCrush で再び true")
    void crush() {
        BeamBlock b = block(100, 0, 0, 0);
        assertThat(b.beginCrush()).isTrue();
        assertThat(b.beginCrush()).isFalse();
        b.endCrush();
        assertThat(b.beginCrush()).isTrue();
    }

    @Test
    @DisplayName("画面外のブロックは当たらず押さない")
    void offScreen() {
        BlockSpec s = new BlockSpec();
        s.setX(5000);
        s.setY(5000);
        BeamBlock b = new BeamBlock(s);
        assertThat(b.hits(new Vector2(5000, 5000), 10)).isFalse();
        assertThat(b.pushOut(new Vector2(5000, 5000), 10, null)).isEqualTo(BeamBlock.Push.NONE);
    }
}
