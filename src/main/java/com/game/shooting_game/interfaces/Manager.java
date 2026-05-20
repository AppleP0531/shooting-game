package com.game.shooting_game.interfaces;

import java.awt.Graphics;

public interface Manager {
    /**
     * セットアップ
     */
    void setup();

    /**
     * 更新
     * 
     * @param dt
     */
    void update(double dt);

    /**
     * 描画
     * 
     * @param g
     */
    void draw(Graphics g);

    /**
     * UIコンポーネントの初期化、レイアウト設定、およびキーバインドの登録
     */
    default void initComponents() {
    }
}