package com.game.shooting_game.interfaces;

import java.awt.Graphics;

public interface GameObject {
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
}