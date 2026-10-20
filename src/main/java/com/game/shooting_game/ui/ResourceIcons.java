package com.game.shooting_game.ui;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;

/**
 * 素材アイコンの取得(1コマ目を指定サイズに縮小して使い回す)
 */
public final class ResourceIcons {
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    private ResourceIcons() {
    }

    /**
     * アイコンの取得
     * 
     * @return アイコン(画像が読めなければnull)
     */
    public static synchronized BufferedImage get(ResourceKinds kind, int size) {
        String key = kind.getResName() + ":" + kind.getRarity() + "@" + size;
        if (cache.containsKey(key))
            return cache.get(key);

        BufferedImage img = null;
        try {
            var map = ResourceManager.getInstance().getResourceImage();
            Map<Rarity, BufferedImage[]> rarityMap = map == null ? null : map.get(kind.getResName());
            BufferedImage[] frames = rarityMap == null ? null : rarityMap.get(kind.getRarity());
            if (frames != null && frames.length > 0 && frames[0] != null)
                img = UIBuilder.resizeImage(frames[0], size);
        } catch (RuntimeException ignored) {
            // 画像が読めない場合はnull(呼び出し側で仮描画)
        }
        cache.put(key, img);
        return img;
    }
}
