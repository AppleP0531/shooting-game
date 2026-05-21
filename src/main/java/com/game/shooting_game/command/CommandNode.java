package com.game.shooting_game.command;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

import com.game.shooting_game.command.CommandGlassPane.CommandAction;

@Data
public class CommandNode {
    private String name;
    private String description;
    private boolean adminOnly;
    private CommandAction action;
    private CommandNode parent;
    private Map<String, CommandNode> subCommands = new HashMap<>();
    private String syntax;

    public CommandNode(CommandNode parent, String name, String description, boolean adminOnly,
            CommandAction action) {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.adminOnly = adminOnly;
        this.action = action;
    }

    /**
     * 使用方法
     * 
     * @return コマンド使用方法
     */
    public String getUseage() {
        if (syntax.isEmpty())
            return getFullPath();
        else
            return getFullPath() + " " + syntax;
    }

    /**
     * 親ノードの名前をたどりつなげる
     * 
     * @return
     */
    private String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " " + name;
    }
}
