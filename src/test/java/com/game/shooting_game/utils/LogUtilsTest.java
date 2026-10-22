package com.game.shooting_game.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.game.shooting_game.utils.LogUtils.Facility;
import com.game.shooting_game.utils.LogUtils.Severity;

class LogUtilsTest {
    @Test
    void priorityはFacilityかけ8たすSeverity() {
        assertEquals(14, LogUtils.priority(Facility.USER, Severity.INFO));
        assertEquals(128 + 3, LogUtils.priority(Facility.LOCAL0, Severity.ERROR));
    }

    @Test
    void TAGは括弧を外し32文字までにする() {
        assertEquals("Command", LogUtils.toTag("[Command]"));
        assertEquals("-", LogUtils.toTag(null));
        assertEquals(32, LogUtils.toTag("x".repeat(50)).length());
    }

    @Test
    void RFC3164の形で整形する() {
        String line = LogUtils.format(Facility.USER, Severity.WARNING,
                LocalDateTime.of(2026, 10, 5, 9, 3, 1), "[Main]", "hello")
                .replaceAll("\\x1b\\[[0-9;]*m", "");
        assertTrue(line.startsWith("<12>Oct  5 09:03:01 "), line);
        assertTrue(line.endsWith("]: hello"), line);
    }

    @Test
    void 閾値より軽いログは出さない() {
        Severity old = LogUtils.getMinSeverity();
        try {
            LogUtils.setMinSeverity(Severity.WARNING);
            assertTrue(LogUtils.isLoggable(Severity.ERROR));
            assertTrue(LogUtils.isLoggable(Severity.WARNING));
            assertFalse(LogUtils.isLoggable(Severity.INFO));
        } finally {
            LogUtils.setMinSeverity(old);
        }
    }
}
