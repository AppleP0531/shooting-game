package com.game.shooting_game.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Data;

import com.game.shooting_game.command.CommandGlassPane.CommandAction;
import com.game.shooting_game.utils.GameUtils;

@Data
public class CommandNode {
    private String name;
    private String description;
    private boolean adminOnly;
    private CommandAction action;
    private CommandNode parent;
    private Map<String, CommandNode> subCommands = new HashMap<>();
    private List<String> syntax = new LinkedList<>();

    public CommandNode(CommandNode parent, String name, String description, boolean adminOnly,
            CommandAction action) {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.adminOnly = adminOnly;
        this.action = action;
    }

    public void setSyntax(String... syntax) {
        for (String placeholder : syntax) {
            this.syntax.add(placeholder);
        }
    }

    /**
     * 使用方法
     * 
     * @return コマンド使用方法
     */
    public String getUseage() {
        if (syntax == null || syntax.isEmpty())
            return getFullPath();
        else {
            String fullpath = getFullPath();
            for (String placeholder : syntax) {
                fullpath = fullpath + " " + placeholder;
            }
            return fullpath;
        }
    }

    /**
     * 親ノードの名前をたどり、すべてつなげて返す
     * 
     * @return
     */
    private String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " " + name;
    }

    /**
     * 最下層子ノードのフルパスをすべて返す
     * 
     * @param adminMode
     * @return
     */
    public List<String> getAllNodes(boolean adminMode) {
        List<String> nodes = new ArrayList<>();

        // 権限チェック
        if (adminOnly && !adminMode)
            return nodes;

        // syntaxがあれば追加
        if (syntax != null && !syntax.isEmpty()) {
            nodes.add(getUseage());
        }

        // サブコマンドがなければ自身のフルパスを返す
        if (subCommands.isEmpty()) {
            if (nodes.isEmpty())
                nodes.add(getUseage());

            return nodes;
        }

        // サブコマンドがあればたどる
        for (CommandNode node : subCommands.values()) {
            for (String useAge : node.getAllNodes(adminMode)) {
                nodes.add(useAge);
            }
        }

        return nodes;
    }

    /**
     * 与えられたトークンから始まるかどうか
     * 
     * @param token
     * @return
     */
    public boolean compersionSyntax(String token) {
        for (String placeholder : syntax) {
            switch (placeholder) {
                case "<x>", "<y>" -> {
                    try {
                        Double.parseDouble(token);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                case "<number>", "<amount>", "<seconds>", "<level>" -> {
                    try {
                        GameUtils.tryParse(token);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                case "<sceneName>", "<stageId>", "<shipId>", "<enemyId>", "<mode>", "<target>", "<effectName>",
                        "<resourceName>", "username", "<password>", "<oldPassword>", "<newPassword>" -> {
                    return true;
                }
            }
        }
        return false;
    }
}
