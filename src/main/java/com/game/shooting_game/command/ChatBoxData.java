package com.game.shooting_game.command;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // 全引数コンストラクタの生成
public class ChatBoxData {
    private List<String> elements;
    private DefaultListModel<String> model;
    private JList<String> list;
    private int maxSavelength;
    private int maxDisplay;
    private int index;
    private int diff;

    /**
     * 履歴追加
     * 
     * @param text 追加内容
     */
    public void addElement(String text) {
        elements.add(text);
        if (elements.size() > maxSavelength)
            elements.removeFirst();
    }
}
