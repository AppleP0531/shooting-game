package com.game.shooting_game.domain.dex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;

class DexServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.create("tester"); // 新規ユーザーデータ(DB不要)
    }

    @Test
    @DisplayName("未遭遇の機体は isSeen が false")
    void notSeenByDefault() {
        assertThat(DexService.isSeen("Enemy1")).isFalse();
    }

    @Test
    @DisplayName("markSeen 後は isSeen が true、何度呼んでも同じ")
    void markSeen() {
        DexService.markSeen("Enemy1");
        DexService.markSeen("Enemy1");
        assertThat(DexService.isSeen("Enemy1")).isTrue();
        assertThat(DexService.isSeen("Enemy2")).isFalse();
    }

    @Test
    @DisplayName("遭遇記録は highScores に seen:ID=1 の形式で保存される")
    void keyFormat() {
        DexService.markSeen("Boss1");
        assertThat(DataManager.getUserData().getProgress().getHighScores()).containsEntry("seen:Boss1", 1);
    }

    @Test
    @DisplayName("null の機体IDは無視され、例外にならない")
    void nullSafety() {
        DexService.markSeen(null);
        assertThat(DexService.isSeen(null)).isFalse();
        assertThat(DexService.seenCount(null)).isZero();
        assertThat(DataManager.getUserData().getProgress().getHighScores()).isEmpty();
    }

    @Test
    @DisplayName("seenCount は一覧のうち遭遇済みの数を数える")
    void seenCount() {
        DexService.markSeen("A");
        DexService.markSeen("C");
        List<String> ids = Arrays.asList("A", "B", "C", null);
        assertThat(DexService.seenCount(ids)).isEqualTo(2);
        assertThat(DexService.seenCount(List.of())).isZero();
    }
}
