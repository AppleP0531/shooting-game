package src.utils;

import src.interfaces.Entity;

/**
 * ゲームユーティリティクラス
 */
public class GameUtils {
    /**
     * 二点間の距離の二乗の取得
     * 
     * @param target1 ターゲット1
     * @param target2 ターゲット2
     * @return 距離の二乗
     */
    public static double distSq(Vector2 target1, Vector2 target2) {
        return Math.pow(target1.getX() - target2.getX(), 2) + Math.pow(target1.getY() - target2.getY(), 2);
    }

    /**
     * target1から見たtarget2のベクトルの取得
     * 
     * @param target1 ターゲット1
     * @param target2 ターゲット2
     * @return ベクトル
     */
    public static double targetVec(Vector2 target1, Vector2 target2) {
        return Math.atan2(target2.getY() - target1.getY(), target2.getX() - target1.getX());
    }

    /**
     * 角度を-PIからPIの範囲に正規化
     * 
     * @param angle 角度
     * @return 正規化された角度
     */
    public static double normalizeAngle(double angle) {
        while (angle >= Math.PI) {
            angle -= Math.PI * 2;
        }
        while (angle <= -Math.PI) {
            angle += Math.PI * 2;
        }
        return angle;
    }

    /**
     * 二つのオブジェクトが当たったかどうか
     * 
     * @param entity1 エンティティ1
     * @param entity2 エンティティ1
     * @return 当たってたらtrue、そうでなければfalse
     */
    public static boolean collidesWith(Entity entity1, Entity entity2) {
        double distSq = GameUtils.distSq(entity1.getPos(), entity2.getPos());
        double radiusSumSq = Math.pow(entity1.getCollisionSize() + entity2.getCollisionSize(), 2);
        return distSq < radiusSumSq;
    }

    /**
     * 時間に基づくアルファ値の計算
     * 
     * @param time 演算用時間
     * @param x    点滅周期
     * @param y    点滅幅
     * @return アルファ値
     */
    public static float calcFlashAlpha(double time, double x, double y) {
        return (float) ((-Math.pow(Math.sin(time * Math.PI * x), 4) + 2 - y) * y);
    }

    /**
     * HH:mm:ssを秒に変換
     * 
     * @param time HH:mm:ss
     * @return 秒
     */
    public static long parseTimeToSeconds(String time) {
        String[] parts = time.split(":");
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        long seconds = Long.parseLong(parts[2]);
        return (hours * 3600) + (minutes * 60) + seconds;
    }

    /**
     * 秒をHH:mm:ssに変換
     * 
     * @param time 秒
     * @return HH:mm:ss
     */
    public static String formatSecondsToTime(long time) {
        long h = time / 3600;
        long m = (time % 3600) / 60;
        long s = time % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}