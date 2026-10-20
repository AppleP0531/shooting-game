package com.game.shooting_game.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatBoxContextTest {
    private ChatBoxContext<String> ctx(int max) {
        return new ChatBoxContext<>(new ArrayList<>(), new DefaultListModel<>(), null, max, 5, 0, 0);
    }

    @Test
    @DisplayName("addElement は上限を超えたら古い順に捨てる")
    void trimsOldest() {
        ChatBoxContext<String> c = ctx(3);
        for (String s : List.of("a", "b", "c", "d", "e"))
            c.addElement(s);
        assertThat(c.getElements()).containsExactly("c", "d", "e");
    }

    @Test
    @DisplayName("上限以内なら全件保持する")
    void keepsAll() {
        ChatBoxContext<String> c = ctx(10);
        c.addElement("x");
        c.addElement("y");
        assertThat(c.getElements()).containsExactly("x", "y");
    }

    @Test
    @DisplayName("LogLine は時刻省略時に現在時刻が入る")
    void logLine() {
        long before = System.currentTimeMillis();
        LogLine l = new LogLine("hi", LogLine.Kind.INFO);
        assertThat(l.createdAt()).isBetween(before, System.currentTimeMillis());
        assertThat(l.text()).isEqualTo("hi");
        assertThat(LogLine.Kind.values()).hasSize(5);
    }
}
