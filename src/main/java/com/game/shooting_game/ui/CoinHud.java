package com.game.shooting_game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Set;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.utils.Vector2;

/**
 * 所持コインの表示(左上に、コインのアイコンと所持数)
 * ゲーム中は「そのステージで拾ったコイン」が同じ場所に出るので、ここでは描かない。
 */
public final class CoinHud {
    // 所持コインを出す画面
    private static final Set<SceneType> SCENES = EnumSet.of(
            SceneType.LOBBY, SceneType.BAG, SceneType.UPGRADE, SceneType.SHOP, SceneType.GACHA,
            SceneType.SELECTSTAGE, SceneType.NORMALSTAGE, SceneType.INFINITYSTAGE,
            SceneType.EXTRA_MODULE, SceneType.EXTRA_BOSS, SceneType.EXTRA_MATERIAL, SceneType.EXTRA_SPECIAL);

    private static BufferedImage icon; // コインのアイコン(最初に描くときに読み込む)

    private CoinHud() {
    }

    /**
     * 描画(対象外の画面では何もしない)
     *
     * @param scene 今の画面
     */
    public static void draw(Graphics g, SceneType scene) {
        if (!SCENES.contains(scene) || DataManager.getUserData() == null)
            return;

        double adj = ShootingGameMain.getScreenSizeAdj();
        Vector2 pos = new Vector2(30 * adj, 15 * adj);

        if (icon == null) {
            var images = ResourceManager.getInstance().getResourceImage();
            ResourceKinds coin = ResourceKinds.COIN;
            if (images != null && images.get(coin.getResName()) != null
                    && images.get(coin.getResName()).get(coin.getRarity()) != null)
                icon = UIBuilder.resizeImage(images.get(coin.getResName()).get(coin.getRarity())[0], 20 * adj);
        }
        if (icon != null)
            UIBuilder.drawImage(g, new Anchor(H.RIGHT, V.CENTER), pos, icon);

        // 管理者モードはコインが無制限なので、数ではなく∞を出す
        String text = DataManager.isAdminMode()
                ? "∞"
                : String.format("%,d", DataManager.getUserData().getResources().getCoins());
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.CENTER), pos, (int) (20 * adj), Color.WHITE, Font.BOLD, text);
    }
}
