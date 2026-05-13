package src.utils.animation;

import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] images; // アニメーションのフレーム画像
    private int currentFrame = 0; // 表示画像のフレーム
    private double frameTimer = 0; // 表示画像フレーム計算タイマー
    private double frameRate; // 画像の切り替え速度

    public Animation(BufferedImage[] images, double frameRate) {
        this.images = images;
        this.frameRate = frameRate;
    }

    /**
     * 更新
     * 
     * @param dt
     */
    public void update(double dt) {
        if (images == null || images.length == 0) {
            return; // 画像がない場合は更新しない
        }

        frameTimer += dt;
        if (frameTimer >= frameRate) {
            currentFrame = 0;
            frameTimer = (currentFrame + 1) % images.length;
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

        return images[currentFrame];
    }
}
