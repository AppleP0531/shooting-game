package com.game.shooting_game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;

/**
 * 画面の部品の見た目を揃えるための共通設定
 * パネルごとに色・文字の大きさ・ボタンの作りがばらばらにならないよう、ここから作る。
 */
public final class UIStyle {
    /** 強化画面のサブパネル(サブ機工房・モジュールなど)の背景。後ろのスキルツリーが透けにくい濃さ */
    public static final Color PANEL_BG = new Color(0, 0, 0, 245);
    public static final Color TEXT = Color.WHITE;
    public static final Color HEADING = new Color(200, 230, 255);
    public static final Color LIST_BG = new Color(20, 24, 34);
    public static final Color LIST_SELECTED = new Color(60, 90, 130);

    private static final String FONT_FAMILY = "SansSerif";

    private UIStyle() {
    }

    /**
     * 基準サイズ(405x720 の画面)の長さを、今の画面の大きさに合わせた画素数にする
     */
    public static int px(double base) {
        return (int) (base * ShootingGameMain.getScreenSizeAdj());
    }

    public static Font font(double size, boolean bold) {
        return new Font(FONT_FAMILY, bold ? Font.BOLD : Font.PLAIN, Math.max(9, px(size)));
    }

    /**
     * 読み取り専用の説明文(背景なし・白文字・自動改行)
     */
    public static JTextArea textArea(double size, boolean bold) {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);
        a.setForeground(TEXT);
        a.setCaretColor(new Color(0, 0, 0, 0));
        a.setFont(font(size, bold));
        return a;
    }

    /**
     * 標準の押しボタン(余白なし・太字)。位置は呼び出し側で決める
     */
    public static JButton button(String text, double fontSize) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setFont(font(fontSize, true));
        return b;
    }

    /**
     * 一覧(JList)を暗い背景にそろえる。空のときに白い箱にならないようにする
     */
    public static void darkList(JList<?> list) {
        list.setBackground(LIST_BG);
        list.setForeground(TEXT);
        list.setSelectionBackground(LIST_SELECTED);
        list.setFont(font(11, true));
    }
}
