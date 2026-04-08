package src.bagScene;

import src.ShootingGameMain;
import src.ResourceManager.ResourceKinds;
import src.utils.Vector2;
import src.utils.UIBuilder;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;

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
        drawEquipmentsList(g, uiData.equipments);
    }

    /**
     * 所有資源リストの描画
     * 
     * @param g
     * @param equipments 所有資源リスト
     */
    private void drawEquipmentsList(Graphics g, Map<ResourceKinds, Integer> equipments) {
        int index = 0;
        for (Map.Entry<ResourceKinds, Integer> entry : equipments.entrySet()) {
            ResourceKinds name = entry.getKey();
            int amount = entry.getValue();
            BufferedImage image = null;

            // 基準点
            Vector2 startPos = new Vector2(
                    50 * ShootingGameMain.getScreenSizeAdj(),
                    50 * ShootingGameMain.getScreenSizeAdj());

            int col = 5; // 一行の個数
            Vector2 posInd = new Vector2(
                    index % col,
                    (int) (index / col));
            Vector2 size = new Vector2(
                    80 * ShootingGameMain.getScreenSizeAdj(),
                    100 * ShootingGameMain.getScreenSizeAdj());
            double spacing = 10 * ShootingGameMain.getScreenSizeAdj();
            Vector2 pos = new Vector2(
                    startPos.getX() + (size.getX() + spacing) * posInd.getX(),
                    startPos.getY() + (size.getY() + spacing) * posInd.getY());
            drawEquipmentsListItem(g, pos, size, image, name, amount);

            index++;
        }
    }

    /**
     * 所有資源リストの各々の項目の描画
     * 
     * @param g
     * @param pos    位置
     * @param image  画像
     * @param name   名前
     * @param amount 個数
     */
    private void drawEquipmentsListItem(Graphics g, Vector2 pos, Vector2 size, BufferedImage image, ResourceKinds name,
            int amount) {
        Vector2 imageSize = new Vector2(
                size.getX() * 3 / 4,
                size.getY() * 3 / 4);
        if (image != null) {

        } else {
            // 枠の描画
            g.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
            g.drawRect((int) (pos.getX() - imageSize.getX() / 2),
                    (int) (pos.getY() - imageSize.getY() / 2),
                    (int) imageSize.getX(),
                    (int) imageSize.getY());

            // noImageテキストの表示
            double textSize = size.getY() / 10;
            UIBuilder.drawString(g, pos, textSize, Color.WHITE, Font.PLAIN, "No Image");
        }

        double spacing = size.getY() / 20;

        double resNameTextSize = size.getY() / 10;
        Vector2 resNameTextPos = new Vector2(
                pos.getX(),
                pos.getY() + imageSize.getY() / 2 + spacing + resNameTextSize / 2);
        UIBuilder.drawString(g, resNameTextPos, resNameTextSize, Color.WHITE, Font.PLAIN, name.toString());

        double amountTextSize = size.getY() / 10;
        Vector2 amountTextPos = new Vector2(
                resNameTextPos.getX(),
                resNameTextPos.getY() + resNameTextSize / 2 + spacing + amountTextSize / 2);
        UIBuilder.drawString(g, amountTextPos, amountTextSize, Color.WHITE, Font.PLAIN, String.valueOf(amount));
    }
}
