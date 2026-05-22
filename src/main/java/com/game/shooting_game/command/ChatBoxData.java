package com.game.shooting_game.command;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // 全引数コンストラクタの生成
public class ChatBoxData {
    private List<String> history;
    private DefaultListModel<String> historyModel;
    private JList<String> historyList;
    private int historyLength;
    private int maxHistoryDisplay;
    private int historyIndex;
    private int historyDiff;

    /**
     * 履歴追加
     * 
     * @param text 追加内容
     */
    public void addElement(String text) {
        history.add(text);
        if (history.size() > historyLength)
            history.removeFirst();
    }
}
