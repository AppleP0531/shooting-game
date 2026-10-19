package com.game.shooting_game.gameScene.beam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ビームブロックの定義(ステージギミック)
 * 
 * 四角いブロックが画面に現れ、指定した方向(上下左右)へビームを撃つ。
 * 機体が前に飛んでいる想定で、ブロックはゆっくり画面の下側へ流れていく。
 * 右から左へ横切るブロックは、右の外側から出て vx を負にする。
 * ビームの色・予兆・太さなどは BeamSpec と同じ考え方。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockSpec {
    private double x = 0; // 出現位置のX(画面中央基準、基準サイズ)。右の外側なら 240 など
    private double y = 80; // 出現位置のY(画面上端基準、基準サイズ)。上の外側なら負の値
    private double vx = 0; // 横の移動速度(基準サイズ/秒)。負なら右から左へ
    private double vy = 20; // 下への移動速度(基準サイズ/秒)。機体が進んでいる演出
    private double size = 30; // 四角の一辺(基準サイズ)
    private String color = "ALT"; // BLUE / RED / ALT(撃つたびに青赤交互)
    private String[] dirs = { "DOWN" }; // 撃つ方向。UP / DOWN / LEFT / RIGHT
    private String pattern = "ALL"; // ALL: 全方向同時 / CYCLE: 1方向ずつ順番に
    private double delay = 1.0; // 最初に撃つまでの時間(秒)
    private double interval = 3.0; // 撃つ間隔(秒)
    private int count = 0; // 撃つ回数。0なら画面にいる間ずっと
    private double warn = 1.0; // 予兆の時間(秒)
    private double duration = 0.7; // 照射の時間(秒)
    private double width = 24; // ビームの太さ
    private double damage = 12; // 1回のダメージ
}
