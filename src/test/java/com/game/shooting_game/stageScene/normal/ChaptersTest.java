package com.game.shooting_game.stageScene.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChaptersTest {
    @Test
    @DisplayName("章は20ステージごと(1〜20が第1章、21が第2章)。0以下は第1章に丸める")
    void chapterOf() {
        assertEquals(1, Chapters.chapterOf(1));
        assertEquals(1, Chapters.chapterOf(20));
        assertEquals(2, Chapters.chapterOf(21));
        assertEquals(2, Chapters.chapterOf(40));
        assertEquals(3, Chapters.chapterOf(41));
        assertEquals(1, Chapters.chapterOf(0));
        assertEquals(1, Chapters.chapterOf(-5));
    }

    @Test
    @DisplayName("章の最後は20の倍数だけ(0以下は違う)")
    void isChapterEnd() {
        assertTrue(Chapters.isChapterEnd(20));
        assertTrue(Chapters.isChapterEnd(40));
        assertFalse(Chapters.isChapterEnd(19));
        assertFalse(Chapters.isChapterEnd(21));
        assertFalse(Chapters.isChapterEnd(0));
        assertFalse(Chapters.isChapterEnd(-20));
    }

    @Test
    @DisplayName("ボスは5の倍数だけ(0以下は違う)")
    void isBoss() {
        assertTrue(Chapters.isBoss(5));
        assertTrue(Chapters.isBoss(10));
        assertTrue(Chapters.isBoss(20));
        assertFalse(Chapters.isBoss(1));
        assertFalse(Chapters.isBoss(6));
        assertFalse(Chapters.isBoss(0));
        assertFalse(Chapters.isBoss(-5));
    }

    @Test
    @DisplayName("章名は星座で、12を超えたら最初から繰り返す。0以下は第1章の名前")
    void name() {
        assertEquals("牡羊座", Chapters.name(1));
        assertEquals("蟹座", Chapters.name(4));
        assertEquals("魚座", Chapters.name(12));
        assertEquals("牡羊座", Chapters.name(13));
        assertEquals("牡牛座", Chapters.name(14));
        assertEquals("魚座", Chapters.name(24));
        assertEquals("牡羊座", Chapters.name(0));
    }

    @Test
    @DisplayName("章ラベルは「第N章・星座」")
    void label() {
        assertEquals("第4章・蟹座", Chapters.label(4));
        assertEquals("第13章・牡羊座", Chapters.label(13));
    }
}
