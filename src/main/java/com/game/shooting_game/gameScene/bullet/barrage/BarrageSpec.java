package com.game.shooting_game.gameScene.bullet.barrage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.gameScene.bullet.Bullet.Direction;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.utils.Vector2;

/**
 * 弾幕(模様を描く連続発射)の定義
 * 
 * 角度は度数で、0が右、90が下。時間経過で回転・スイープさせて模様を作る。
 * 各弾の色は color で指定する。
 * - BLUE / RED: 全部その色
 * - ALT_ARM: 1発ごとに青赤交互
 * - ALT_VOLLEY: 1回の発射ごとに青赤交互
 * - ALT_SEGMENT: 花びら(FLOWER)/辺(POLYGON)ごとに青赤交互
 */
@Data // Getter, Setterの自動生成
@NoArgsConstructor // 引数なしコンストラクタの生成
@JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
public class BarrageSpec {
    /**
     * 模様の種類
     */
    public static enum Type {
        RING, // 全周に等間隔(count発)。spinで回転させると渦巻きになる
        SPIRAL, // RINGと同じ(間隔を短く・spinを大きくして渦巻きを作る用の別名)
        FAN, // 扇状(spread度の範囲にcount発)。aimとsweepで狙い・振りを付ける
        FLOWER, // 花形(petals枚の花びら)。角度ごとに速度を変えて広がる花を描く
        POLYGON, // 多角形(sides角形)。広がる多角形を描く
        CURTAIN, // 画面幅いっぱいの弾の幕(gap幅の隙間がsweepSpeedで左右に動く)
        ANCHOR // 照準点を予告してから、そこを中心にburstの形で弾を爆発させる
    }

    private Type type = Type.RING;
    private BulletKinds bulletKind = BulletKinds.STRAIGHT; // 弾の種類
    private int bulletId = 1; // 弾ID(BulletConfigのBOSS)
    private Direction direction = Direction.RIGHT; // CURVE/WAVE用の曲がる向き
    private String color = "ALT_ARM"; // 色の割り当て
    private double interval = 1.0; // 発射間隔(秒)
    private int count = 12; // 1回の発射数(RING/SPIRAL/FAN/FLOWER/POLYGON)、CURTAINでは列数
    private double startAngle = 0; // 開始角度(度)
    private double spin = 0; // 回転速度(度/秒)
    private double speed = 1.0; // 弾速倍率
    private double delay = 0; // 開始までの時間(秒)
    private double onTime = 0; // 撃つ時間(秒)。offTimeと両方指定で撃つ/休むを繰り返す
    private double offTime = 0; // 休む時間(秒)
    private Vector2 offset = new Vector2(0, 0); // 発射位置のずれ(ボス中心から)
    private String aim = "down"; // FAN: 狙い("down" or "target")
    private double spread = 60; // FAN: 扇の開き(度)
    private double sweepAmp = 0; // FAN: 狙いを左右に振る幅(度)
    private double sweepSpeed = 1; // FAN/CURTAIN: 振る速さ(ラジアン/秒)
    private int petals = 5; // FLOWER: 花びらの数
    private double depth = 0.5; // FLOWER: 花びらの深さ(0~1)
    private int sides = 4; // POLYGON: 辺の数
    private double gap = 0.25; // CURTAIN: 隙間の幅(画面幅に対する割合)
    private String anchor = "player"; // ANCHOR: 照準する点("player"=発射時の自機の位置、"fixed"=anchorPosの位置、"random"=画面の上半分のランダムな点)
    private Vector2 anchorPos = new Vector2(0, 200); // ANCHOR(fixed): 画面中央からの横ずれ、画面上端からの縦の位置(基準サイズ)
    private double warnTime = 1.0; // ANCHOR: 予告の時間(秒)
    private Type burst = Type.RING; // ANCHOR: 爆発の形(RING/FLOWER/POLYGON/FAN)
}
