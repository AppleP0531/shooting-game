package com.game.shooting_game.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSettingsSlowTest {
    private static final Path FILE = Path.of("settings.properties");
    private boolean existed;
    private byte[] original;
    private int originalValue;

    @BeforeEach
    void setUp() throws Exception {
        existed = Files.exists(FILE);
        original = existed ? Files.readAllBytes(FILE) : null;
        originalValue = GameSettings.getSlowPercent();
    }

    @AfterEach
    void tearDown() throws Exception {
        GameSettings.setSlowPercent(originalValue);
        if (existed)
            Files.write(FILE, original);
        else
            Files.deleteIfExists(FILE);
    }

    @Test
    @DisplayName("低速移動の割合は 10〜90 に丸まる")
    void clamped() {
        GameSettings.setSlowPercent(30);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(30);
        GameSettings.setSlowPercent(5);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(10);
        GameSettings.setSlowPercent(-100);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(10);
        GameSettings.setSlowPercent(150);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(90);
        GameSettings.setSlowPercent(10);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(10);
        GameSettings.setSlowPercent(90);
        assertThat(GameSettings.getSlowPercent()).isEqualTo(90);
    }

    @Test
    @DisplayName("定数: 範囲は 10〜90、既定は 50")
    void constants() {
        assertThat(GameSettings.DEFAULT_SLOW_PERCENT).isEqualTo(50);
        assertThat(GameSettings.SLOW_PERCENT_MIN).isEqualTo(10);
        assertThat(GameSettings.SLOW_PERCENT_MAX).isEqualTo(90);
    }
}
