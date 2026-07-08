package com.game.shooting_game.utils.animation;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

import com.game.shooting_game.interfaces.Manager;

public final class AnimationManager implements Manager {
    private static AnimationManager instance;

    @Getter
    Map<String, Animation> animationCache = new HashMap<>();

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
     * アニメーションを追加
     * 
     * @param animation アニメーション情報
     * 
     * @return 識別用ID
     */
    public String addAnimation(Animation animation) {
        // 重複しないIDの作成
        String id = UUID.randomUUID().toString();
        animationCache.put(id, animation);
        return id;
    }

    /**
     * アニメーションの削除
     * 
     * @param id
     */
    public void removeAnimation(String id) {
        animationCache.remove(id);
    }

    /**
     * 現在の表示画像の取得
     * 
     * @param id
     * @return
     */
    public BufferedImage getCurrentImage(String id) {
        return animationCache.get(id).getCurrentImage();
    }

    /**
     * 現在の表示フレーム番号の取得
     * 
     * @param id
     * @return
     */
    public int getCurrentFrameNum(String id) {
        return animationCache.get(id).getCurrentFrameNum();
    }
}
