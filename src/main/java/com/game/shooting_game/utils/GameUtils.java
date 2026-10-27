package com.game.shooting_game.utils;

import com.game.shooting_game.interfaces.Entity;

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
     * 数字に変換
     * 
     * @param value 値
     * @return 変換できた場合は値、できなかった場合はnull
     */
    public static Integer tryParse(String value) throws NumberFormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw e;
        }
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

    // ----- ダメージ計算の式(ゲームの状態を読まない純粋な計算。値の集め方は DamageCalculator が担当する) -----
    /** 割合の軽減率の上限 */
    public static final double MAX_DAMAGE_REDUCTION = 0.8;
    /** どれだけ防御を固めても、元のダメージのうちこの割合は通る */
    public static final double MIN_DAMAGE_PASS = 0.3;

    /**
     * 攻撃側の火力
     *
     * @param base 基礎ダメージ
     * @param rate 火力倍率
     * @param flat 実数値の攻撃力(倍率のあとに足す)
     * @return 基礎ダメージ × 火力倍率 + 実数値の攻撃力
     */
    public static double attackDamage(double base, double rate, double flat) {
        return base * rate + flat;
    }

    /**
     * 敵が受けるダメージ
     *
     * @param attack     攻撃側の火力
     * @param colorRate  色による倍率
     * @return 火力 × 色の倍率(敵の硬さはHPで表すので、ここでは割らない)
     */
    public static double damageToEnemy(double attack, double colorRate) {
        return attack * colorRate;
    }

    /**
     * 自機が受けるダメージ
     * 割合の軽減(上限あり) → 敵が強い条件の倍率 → 実数値の防御力を引く、の順で計算する。
     * 計算結果が小さくなりすぎても、元のダメージの {@link #MIN_DAMAGE_PASS} は通る。
     *
     * @param raw         軽減前のダメージ
     * @param reduceRate  割合の軽減率(0〜{@link #MAX_DAMAGE_REDUCTION}に収める)
     * @param damageRate  敵が強い条件の倍率(軽減のあとにかける)
     * @param flatDefense 実数値の防御力(割合の軽減のあとに引く)
     * @return 受けるダメージ(軽減前が0以下ならそのまま返す)
     */
    public static double damageToPlayer(double raw, double reduceRate, double damageRate, double flatDefense) {
        if (raw <= 0)
            return raw;
        double v = raw * (1 - Math.min(MAX_DAMAGE_REDUCTION, Math.max(0, reduceRate)));
        v *= damageRate;
        v -= flatDefense;
        return Math.max(raw * MIN_DAMAGE_PASS, v);
    }
}