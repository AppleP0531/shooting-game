package com.game.shooting_game.gameScene.beam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ビームの定義(ステージギミック・敵・ボス・自機・サブ機で共通)
 * 
 * 流れ: 予兆(warn、ダメージなし)→ 照射(duration、ダメージあり)→ 消滅(fade、ダメージなし)
 * 角度は度数で、0が右、90が下、-90が上。
 * 色は color で指定する。
 * - BLUE / RED: その色
 * - ALT: 撃つたびに青赤交互
 * - RANDOM: ランダム
 * - PLAYER: 撃った機体(自機)の今の色に追従(自機・サブ機用)
 * 敵のビームは自機と同じ色なら当たらない(弾と同じルール。スペースキーで色を変えてかわせる)。
 */
@Data // Getter, Setterの自動生成
@NoArgsConstructor // 引数なしコンストラクタの生成
@JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
public class BeamSpec {
    /**
     * 発射位置の種類
     */
    public static enum Origin {
        TOP_PLAYER, // 画面上から。予兆の間は自機のX座標を追い、直前で固定する
        TOP_SELF, // 画面上から。撃った敵のX座標(背後=画面上から降ってくる)
        TOP_X, // 画面上から。x(画面中央基準)で指定
        TOP_RANDOM, // 画面上から。Xはランダム
        WALL_LEFT, // 左の壁から右へ。y(画面上端基準)で指定
        WALL_RIGHT, // 右の壁から左へ。y(画面上端基準)で指定
        WALL_RANDOM, // 左右どちらかの壁から、ランダムな高さで
        SHIP_AIM, // 撃った機体から自機へ向けて。予兆の間は自機を追う(ボス用)
        SHIP_UP // 撃った機体から真上へ(自機・サブ機用)
    }

    private Origin origin = Origin.TOP_PLAYER;
    private String color = "ALT"; // 色の割り当て
    private double x = 0; // TOP_X: 画面中央基準のX / TOP_SELF: 敵からのずれ
    private double y = 0; // WALL_LEFT/WALL_RIGHT: 画面上端基準のY(WALL_RANDOMでは使わない)
    private double angle = 0; // 角度の補正(度)
    private double sweep = 0; // 照射中に振る角度(度)。-sweep/2 から +sweep/2 へ回る
    private double warn = 1.2; // 予兆の時間(秒)
    private double duration = 0.9; // 照射の時間(秒)
    private double fade = 0.2; // 消えるまでの時間(秒)
    private double width = 36; // 太さ
    private double damage = 15; // 敵のビーム: 当たったときのダメージ / 自機のビーム: 1秒あたりのダメージ
    private double delay = 0; // 最初に撃つまでの時間(秒)
    private double interval = 0; // 発射間隔(秒)。0なら1回だけ
    private int count = 0; // 発射回数。0なら無制限(intervalが0より大きいとき)
    private int minLevel = 0; // 自機のビーム: 撃てる機体レベル
    private double bossRate = 0.5; // 自機のビーム: ボスに対するダメージ倍率
    private double lock = 0.65; // 予兆のうち、追従をやめて狙いを固定する割合
}
