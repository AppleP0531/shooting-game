package com.game.shooting_game.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameUtilsTest {
    @Test
    @DisplayName("distSq は距離の二乗を返す")
    void distSq() {
        assertThat(GameUtils.distSq(new Vector2(0, 0), new Vector2(3, 4))).isEqualTo(25.0);
        assertThat(GameUtils.distSq(new Vector2(1, 1), new Vector2(1, 1))).isZero();
    }

    @Test
    @DisplayName("targetVec は target1 から見た target2 の角度を返す")
    void targetVec() {
        Vector2 o = new Vector2(0, 0);
        assertThat(GameUtils.targetVec(o, new Vector2(1, 0))).isCloseTo(0, within(1e-9));
        assertThat(GameUtils.targetVec(o, new Vector2(0, 1))).isCloseTo(Math.PI / 2, within(1e-9));
        assertThat(GameUtils.targetVec(o, new Vector2(-1, 0))).isCloseTo(Math.PI, within(1e-9));
    }

    @Test
    @DisplayName("normalizeAngle は (-PI, PI] の範囲に収める")
    void normalizeAngle() {
        assertThat(GameUtils.normalizeAngle(3 * Math.PI)).isCloseTo(Math.PI, within(1e-9));
        assertThat(GameUtils.normalizeAngle(0.5)).isEqualTo(0.5);
        assertThat(GameUtils.normalizeAngle(2 * Math.PI + 0.25)).isCloseTo(0.25, within(1e-9));
        assertThat(GameUtils.normalizeAngle(-2 * Math.PI - 0.25)).isCloseTo(-0.25, within(1e-9));
        assertThat(Math.abs(GameUtils.normalizeAngle(-3 * Math.PI))).isCloseTo(Math.PI, within(1e-9));
    }

    @Test
    @DisplayName("tryParse は整数を変換し、不正な文字列は NumberFormatException")
    void tryParse() {
        assertThat(GameUtils.tryParse("42")).isEqualTo(42);
        assertThat(GameUtils.tryParse("-7")).isEqualTo(-7);
        assertThatThrownBy(() -> GameUtils.tryParse("abc")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> GameUtils.tryParse("")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("時刻文字列と秒数が相互に変換できる")
    void timeConversion() {
        assertThat(GameUtils.parseTimeToSeconds("00:00:00")).isZero();
        assertThat(GameUtils.parseTimeToSeconds("01:02:03")).isEqualTo(3723);
        assertThat(GameUtils.formatSecondsToTime(3723)).isEqualTo("01:02:03");
        assertThat(GameUtils.formatSecondsToTime(0)).isEqualTo("00:00:00");
        assertThat(GameUtils.formatSecondsToTime(100 * 3600 + 59)).isEqualTo("100:00:59");
        for (long s : new long[] { 1, 59, 60, 3599, 3600, 86399, 123456 })
            assertThat(GameUtils.parseTimeToSeconds(GameUtils.formatSecondsToTime(s))).isEqualTo(s);
    }

    @Test
    @DisplayName("parseTimeToSeconds は形式不正で例外")
    void parseTimeInvalid() {
        assertThatThrownBy(() -> GameUtils.parseTimeToSeconds("12:30"))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> GameUtils.parseTimeToSeconds("a:b:c")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("calcFlashAlpha は 0〜1 の範囲に収まる")
    void flashAlpha() {
        for (double t = 0; t < 3; t += 0.07) {
            float a = GameUtils.calcFlashAlpha(t, 2, 0.5);
            assertThat(a).isBetween(0f, 1f);
        }
    }
}
