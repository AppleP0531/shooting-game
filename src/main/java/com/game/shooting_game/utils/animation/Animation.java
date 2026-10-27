package com.game.shooting_game.utils.animation;

import java.awt.image.BufferedImage;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.ui.UIBuilder;

public class Animation {
    private AnimationLogic customLogic = null;
    private BufferedImage[] images; // アニメーションのフレーム画像
    private int currentFrame = 0; // 表示画像のフレーム
    @Getter
    @Setter
    private double frameTimer = 0; // 表示画像フレーム計算タイマー
    @Getter
    @Setter
    private double frameRate; // 画像の切り替え速度

    @FunctionalInterface
    public interface AnimationLogic {
        int calculateFrame(int currentFrame, Animation context);
    }

    public Animation(BufferedImage[] images, double size, double frameRate) {
        // 画像が存在しなければ無視
        if (images == null || images.length == 0)
            return;

        // 画像をリサイズして登録
        for (int i = 0; i < images.length; i++) {
            images[i] = UIBuilder.resizeImage(images[i], size);
        }
        this.images = images;
        this.frameRate = frameRate;
    }

    /**
     * カスタムロジックの注入
     * 
     * @param logic
     */
    public void setCustomLogic(AnimationLogic logic) {
        this.customLogic = logic;
    }

    /**
     * 更新
     * 
     * @param dt
     */
    public void update(double dt) {
        if (images == null || images.length == 0)
            return; // 画像がない場合は更新しない

        frameTimer += dt;

        if (customLogic != null) {
            currentFrame = customLogic.calculateFrame(currentFrame, this);
        } else {
            if (frameTimer >= frameRate) {
                currentFrame = (currentFrame + 1) % images.length;
                frameTimer = 0;
            }
        }
    }

    /**
     * 現在のフレーム画像の取得
     * 
     * @return
     */
    public BufferedImage getCurrentImage() {
        if (images == null || images.length == 0) {
            return null; // 画像がない場合はnullを返す
        }

        // コマ数より大きい番号になっても止まらないよう、最後のコマにとどめる(ダミー画像などで、コマ数が設定より少ないとき)
        return images[Math.min(Math.max(currentFrame, 0), images.length - 1)];
    }

    /**
     * 現在のフレーム番号の取得
     * 
     * @return
     */
    public int getCurrentFrameNum() {
        if (images == null || images.length == 0) {
            return 0; // 画像がない場合は0を返す
        }

        return currentFrame;
    }
}
