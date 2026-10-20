package com.game.shooting_game.command;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * チャットボックス(入力履歴・ログ・予測変換)1つ分のデータ
 *
 * @param <T> 1行の型(入力履歴・予測変換は String、ログは LogLine)
 */
@Data
@AllArgsConstructor // 全引数コンストラクタの生成
public class ChatBoxContext<T> {
    private List<T> elements;
    private DefaultListModel<T> model;
    private JList<T> list;
    private int maxSavelength;
    private int maxDisplay;
    private int index;
    private int diff;

    /**
     * 履歴追加
     * 
     * @param element 追加内容
     */
    public void addElement(T element) {
        elements.add(element);
        if (elements.size() > maxSavelength)
            elements.removeFirst();
    }
}
