package com.game.shooting_game.utils.animation;

import com.game.shooting_game.interfaces.Manager;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import lombok.Getter;

public final class AnimationManager implements Manager {
    private static AnimationManager instance;

    @Getter
    Map<Double, Animation> animationCache = new HashMap<>();

    private AnimationManager() {
        AnimationManager.instance = this;
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static AnimationManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, AnimationManager::new);
    }

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {
        // キャッシュ内の全アニメーションを更新
        animationCache.values().forEach(animation -> animation.update(dt));
    }

    @Override
    public void draw(Graphics g) {

    }

    /**
     * アニメーションの追加
     * 
     * @param id        アニメーションのID
     * @param animation アニメーション情報
     */
    public void addAnimation(double id, Animation animation) {
        if (animationCache.get(id) != null)
            return;

        animationCache.put(id, animation);
    }

    /**
     * アニメーションの削除
     * 
     * @param id
     */
    public void removeAnimation(double id) {
        animationCache.remove(id);
    }

    /**
     * 現在の表示画像の取得
     * 
     * @param id
     * @return
     */
    public BufferedImage getCurrentImage(double id) {
        return animationCache.get(id).getCurrentImage();
    }
}
