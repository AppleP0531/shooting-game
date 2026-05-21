package com.game.shooting_game.command;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class CommandTrie {
    private boolean adminOnly;
    private Map<String, CommandTrie> subNodes = new HashMap<>();

    public CommandTrie(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }
}
