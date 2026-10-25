package com.game.shooting_game.gameScene.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 隕石の定義(ステージギミック)
 *
 * 画面の上から降ってくる障害物。自機に当たるとダメージ、自機の弾でHPを削って壊せる(敵の弾は素通りする)。
 * 位置・大きさは基準サイズ(画面倍率をかける前)。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeteorSpec {
    private Double x = null; // 出現位置のX(画面中央基準)。省略ならランダム
    private double y = -30; // 出現位置のY(画面上端基準)
    private double vx = 0; // 横の速さ(基準サイズ/秒)
    private double vy = 110; // 落ちる速さ(基準サイズ/秒)
    private double radius = 18; // 半径
    private double hp = 40; // HP(ステージの敵の硬さの倍率がかかる)
    private double damage = 18; // 自機に当たったときのダメージ
    private double delay = 1.0; // 最初に出すまでの時間(秒)
    private double interval = 0; // 出す間隔(秒)。0なら1回だけ
    private int count = 1; // 出す個数。0なら無制限(intervalが0より大きいとき)
}
