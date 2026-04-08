package src.interfaces;

import java.awt.Graphics;

/**
 * void update(double);
 * void draw(Graphics);
 */
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