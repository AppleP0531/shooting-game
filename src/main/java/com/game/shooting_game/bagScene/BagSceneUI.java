package com.game.shooting_game.bagScene;

import java.awt.Graphics;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;

public class BagSceneUI {

    public record BagSceneUIData(
            Map<ResourceKinds, Integer> equipments) {
    }

    public BagSceneUI() {

    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g, BagSceneUIData uiData) {

    }
}
