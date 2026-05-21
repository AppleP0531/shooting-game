package com.game.shooting_game.command;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

import com.game.shooting_game.command.CommandGlassPane.CommandAction;

/**
 * コマンドノード
 */
@Data
public class CommandNode {
    private String description;
    private boolean adminOnly;
    private CommandAction action;
    private Map<String, CommandNode> subCommands = new HashMap<>();

    public CommandNode(String description, boolean adminOnly, CommandAction action) {
        this.description = description;
        this.adminOnly = adminOnly;
        this.action = action;
    }
}
