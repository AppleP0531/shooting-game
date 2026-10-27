package com.game.shooting_game.gameScene.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusSetTest {
    @Test
    @DisplayName("かけた効果は時間で切れ、切れるまでの継続ダメージが返る")
    void expires() {
        StatusSet s = new StatusSet();
        s.apply(StatusEffect.BURN, 2.0);
        assertThat(s.isActive(StatusEffect.BURN)).isTrue();
        assertThat(s.update(1.0)).isCloseTo(StatusEffect.BURN.getTickRate() * 1.0, within(1e-12));
        // 残り1秒のところへ3秒進めても、効果のあった1秒ぶんしかダメージにならない
        assertThat(s.update(3.0)).isCloseTo(StatusEffect.BURN.getTickRate() * 1.0, within(1e-12));
        assertThat(s.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("重ねがけは、時間が長い方になる")
    void refresh() {
        StatusSet s = new StatusSet();
        s.apply(StatusEffect.POISON, 5.0);
        s.apply(StatusEffect.POISON, 2.0);
        assertThat(s.remaining(StatusEffect.POISON)).isEqualTo(5.0);
        s.apply(StatusEffect.POISON, 8.0);
        assertThat(s.remaining(StatusEffect.POISON)).isEqualTo(8.0);
    }

    @Test
    @DisplayName("強さの倍率は、継続ダメージにかかる")
    void power() {
        StatusSet s = new StatusSet();
        s.apply(StatusEffect.POISON, 4.0, 2.0);
        assertThat(s.update(1.0)).isCloseTo(StatusEffect.POISON.getTickRate() * 2.0, within(1e-12));
    }

    @Test
    @DisplayName("0秒以下・nullの効果は無視される")
    void ignoreInvalid() {
        StatusSet s = new StatusSet();
        s.apply(null, 3.0);
        s.apply(StatusEffect.SLOW, 0);
        s.apply(StatusEffect.SLOW, -1);
        assertThat(s.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("動く速さ: 鈍足は半分、感電は止まる、なければ1")
    void speedRate() {
        StatusSet s = new StatusSet();
        assertThat(s.speedRate()).isEqualTo(1.0);
        s.apply(StatusEffect.SLOW, 3.0);
        assertThat(s.speedRate()).isEqualTo(0.5);
        s.apply(StatusEffect.SHOCK, 1.0);
        assertThat(s.speedRate()).isEqualTo(0.0);
        s.update(2.0); // 感電だけ切れる
        assertThat(s.speedRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("毒の間は回復できない")
    void poisonBlocksHeal() {
        StatusSet s = new StatusSet();
        assertThat(s.blocksHeal()).isFalse();
        s.apply(StatusEffect.BURN, 3.0);
        assertThat(s.blocksHeal()).isFalse();
        s.apply(StatusEffect.POISON, 3.0);
        assertThat(s.blocksHeal()).isTrue();
    }

    @Test
    @DisplayName("脆弱の間は、受けるダメージが増える(強さで増え幅が変わる)")
    void vulnerable() {
        StatusSet s = new StatusSet();
        assertThat(s.damageTakenRate()).isEqualTo(1.0);
        s.apply(StatusEffect.VULNERABLE, 5.0);
        assertThat(s.damageTakenRate()).isCloseTo(StatusEffect.VULNERABLE_RATE, within(1e-12));
        s.apply(StatusEffect.VULNERABLE, 5.0, 2.0);
        assertThat(s.damageTakenRate()).isCloseTo(1.0 + (StatusEffect.VULNERABLE_RATE - 1.0) * 2.0, within(1e-12));
        s.update(10.0);
        assertThat(s.damageTakenRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("設定の名前から効果を読める(大文字小文字は問わず、不明は null)")
    void parse() {
        assertThat(StatusEffect.parse("burn")).isEqualTo(StatusEffect.BURN);
        assertThat(StatusEffect.parse(" SHOCK ")).isEqualTo(StatusEffect.SHOCK);
        assertThat(StatusEffect.parse("nope")).isNull();
        assertThat(StatusEffect.parse(null)).isNull();
        assertThat(StatusEffect.parse("")).isNull();
    }
}
