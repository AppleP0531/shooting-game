package src.interfaces;

import java.awt.Graphics;

/**
 * void setup();
 * void update(double);
 * void draw(Graphics);
 */
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