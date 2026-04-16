package src.interfaces;

import src.utils.Vector2;

public interface Entity {
    /**
     * 位置の取得
     * 
     * @return 位置座標
     */
    Vector2 getPos();

    /**
     * 当たり判定の大きさ取得
     * 
     * @return 当たり判定の大きさ
     */
    double getCollisionSize();
}