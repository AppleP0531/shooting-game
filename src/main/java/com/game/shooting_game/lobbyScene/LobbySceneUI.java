package com.game.shooting_game.lobbyScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.module.Module;
import com.game.shooting_game.module.ModuleService;
import com.game.shooting_game.module.ModuleStat;
import com.game.shooting_game.skill.SkillService;
import com.game.shooting_game.skill.UnitService;
import com.game.shooting_game.subship.SubShipRenderer;
import com.game.shooting_game.subship.SubShipService;
import com.game.shooting_game.utils.CostPanel;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.Anchor.H;
import com.game.shooting_game.utils.Anchor.V;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class LobbySceneUI {
    private final ShootingGameMain MAIN;
    private final GameManager GM;
    private final BulletManager BM;
    private final ShipManager SHIP_M;

    private String latestViewingShipName;
    private double levelUpInterval; // 強化の間隔
    private double levelUpIntervalTimer; // タイマー

    public record HangarSceneUIData(
            String[] ships,
            int ind) {
    }

    public LobbySceneUI() {
        MAIN = ShootingGameMain.getInstance();
        GM = GameManager.getInstance();
        BM = BulletManager.getInstance();
        SHIP_M = ShipManager.getInstance();

        // 2秒ごとにレベルアップする
        levelUpIntervalTimer = levelUpInterval = 2;
    }

    /**
     * セットアップ
     */
    public void setup() {
        SHIP_M.clear("all");
        BeamManager.getInstance().clear(); // 前のプレビューのビームを消す
        // ターゲットとなる敵を生成
        SHIP_M.addShip(ShipFactory.createShip(
                ShipKinds.ENEMY,
                "Enemy1",
                ColorMode.BLUE,
                0,
                new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() / 4),
                new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() / 4)));
        // HPを無限大に設定(不死にする)
        SHIP_M.getEnemyList().getFirst().setHp(Double.POSITIVE_INFINITY);
        levelUpIntervalTimer = levelUpInterval;
        latestViewingShipName = null;
    }

    /**
     * 更新
     * 
     * @param dt
     */
    public void update(double dt) {
        levelUpIntervalTimer -= dt;
        if (levelUpIntervalTimer <= 0) {
            levelUpIntervalTimer = levelUpInterval;
            // 暴走状態でなければレベルを上げる、暴走状態であればレベルをリセット
            if (SHIP_M.getPlayer().getShipLevel() < 5)
                SHIP_M.getPlayer().levelUp();
            else
                SHIP_M.getPlayer().setShipLevel(0);
        }

        GM.checkCollisions();
        BM.update(dt);
        BeamManager.getInstance().update(dt); // 自機・サブ機のビームもプレビューで見られる
        SHIP_M.update(dt);
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g, HangarSceneUIData uiData) {
        drawPreviewShip(g, uiData.ships[uiData.ind]);
        drawTabBar(g, uiData.ships.length, uiData.ind);
        drawUserLevel(g);
        drawEquipment(g);
    }

    /**
     * 船のプレビューを描画
     * 
     * @param g
     */
    private void drawPreviewShip(Graphics g, String shipName) {
        // 参照中の船が切り替わった時に更新
        if (shipName != latestViewingShipName) {
            BeamManager.getInstance().clear(); // 前の船のビームは消す
            SHIP_M.setPlayer(new Player(
                    shipName,
                    ColorMode.BLUE,
                    new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() - 160 * ShootingGameMain.getScreenSizeAdj())));
        }

        BM.draw(g);
        BeamManager.getInstance().draw(g);
        SHIP_M.draw(g);

        latestViewingShipName = shipName;
    }

    /**
     * タブバーの描画
     */
    private void drawTabBar(Graphics g, int totalLength, int ind) {
        Vector2 barPos = new Vector2(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - 130 * ShootingGameMain.getScreenSizeAdj());
        Vector2 barSize = new Vector2(
                120,
                10);

        for (int i = 0; i < totalLength; i++) {
            double distance = (barSize.getX() - barSize.getY() * totalLength) / (totalLength - 1);
            double startX = barPos.getX() - barSize.getX() / 2 + i * (barSize.getY() + distance);
            double startY = barPos.getY() - barSize.getY() / 2;

            // 参照中の船がわかるバーの描画
            g.setColor(Color.CYAN);
            g.drawPolygon(
                    new int[] {
                            (int) (startX + barSize.getY() / 2 - 4 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startX + barSize.getY() / 2),
                            (int) (startX + barSize.getY() / 2 + 4 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startX + barSize.getY() / 2)
                    },
                    new int[] {
                            (int) (startY + barSize.getY() / 2),
                            (int) (startY + barSize.getY() / 2 - 6 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startY + barSize.getY() / 2),
                            (int) (startY + barSize.getY() / 2 + 6 * ShootingGameMain.getScreenSizeAdj())
                    },
                    4);
            if (i == ind) {
                g.fillPolygon(
                        new int[] {
                                (int) (startX + barSize.getY() / 2 - 4 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startX + barSize.getY() / 2),
                                (int) (startX + barSize.getY() / 2 + 4 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startX + barSize.getY() / 2)
                        },
                        new int[] {
                                (int) (startY + barSize.getY() / 2),
                                (int) (startY + barSize.getY() / 2 - 6 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startY + barSize.getY() / 2),
                                (int) (startY + barSize.getY() / 2 + 6 * ShootingGameMain.getScreenSizeAdj())
                        },
                        4);
            }
        }
    }

    /**
     * ユーザーレベルの描画
     * 
     * @param g
     */

    private void drawUserLevel(Graphics g) {
        int level = DataManager.getUserLevel().level();
        int exp = DataManager.getUserLevel().exp();
        int maxExp = 0;
        if (level < DataManager.getExpData().size())
            maxExp = DataManager.getExpData().get(level);

        String text = "User Level: " + level + " , Exp: " + exp + "/" + maxExp;
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(0, 0), 20, Color.WHITE, Font.PLAIN, text);
    }

    /**
     * 現在の装備一覧の描画(左上のカード)
     * 自機(Lv・★)、サブ機(左下と右上にずらして2機)、装甲(HP)、火力、無敵時間、暴走時間、モジュール枠。
     * モジュールは装着中のものを種類の文字とレア度の色で出す。
     * 
     * @param g
     */
    private void drawEquipment(Graphics g) {
        if (DataManager.getUserData() == null)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String shipId = DataManager.getUserData().getSelectedShip();
        int x = (int) (6 * adj);
        int y = (int) (30 * adj);
        int w = (int) (150 * adj);
        int h = (int) (238 * adj);
        int pad = (int) (6 * adj);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(110, 210, 255, 170));
        g2.drawRoundRect(x, y, w, h, 12, 12);

        Font bold = new Font("SansSerif", Font.BOLD, Math.max(9, (int) (11 * adj)));
        Font small = new Font("SansSerif", Font.PLAIN, Math.max(8, (int) (10 * adj)));
        g2.setFont(bold);
        g2.setColor(new Color(150, 225, 255));
        g2.drawString("装備", x + pad, y + (int) (13 * adj));

        // --- 自機アイコン + Lv・★ ---
        int icon = (int) (38 * adj);
        int iy = y + (int) (20 * adj);
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(x + pad, iy, icon, icon, 8, 8);
        BufferedImage[] frames = SHIP_M.getShipImage().get(shipId);
        if (frames != null && frames.length > 0 && frames[0] != null) {
            BufferedImage img = frames[0];
            double k = Math.min((double) icon / img.getWidth(), (double) icon / img.getHeight()) * 0.9;
            int dw = (int) (img.getWidth() * k);
            int dh = (int) (img.getHeight() * k);
            g2.drawImage(img, x + pad + (icon - dw) / 2, iy + (icon - dh) / 2, dw, dh, null);
        }
        g2.setColor(new Color(255, 214, 80, 200));
        g2.drawRoundRect(x + pad, iy, icon, icon, 8, 8);
        g2.setFont(bold);
        g2.setColor(Color.WHITE);
        g2.drawString(shipId + "  Lv" + UnitService.getLevel(shipId), x + pad + icon + (int) (6 * adj),
                iy + (int) (14 * adj));
        g2.setColor(new Color(255, 214, 80));
        g2.drawString(UnitService.starText(shipId), x + pad + icon + (int) (6 * adj), iy + (int) (30 * adj));

        // --- サブ機(左下と右上にずらして置く) ---
        int sy = iy + icon + (int) (6 * adj);
        int cell = (int) (50 * adj);
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(x + pad, sy, cell + (int) (8 * adj), cell, 8, 8);
        double time = System.nanoTime() / 1e9;
        boolean equipped = SubShipService.isEquipped() && SubShipService.hullTier() > 0;
        if (equipped) {
            int hull = SubShipService.hullTier();
            int cannon = SubShipService.getTier("CANNON");
            int engine = SubShipService.getTier("ENGINE");
            int core = SubShipService.getTier("CORE");
            int emitter = SubShipService.getTier("EMITTER");
            double sc = 0.55;
            // 左機は左下、右機は右上
            SubShipRenderer.draw(g2, x + pad + cell * 0.30, sy + cell * 0.68, true,
                    hull, cannon, engine, core, emitter, ColorMode.BLUE, time, 1f, sc);
            SubShipRenderer.draw(g2, x + pad + cell * 0.30 + cell * 0.45, sy + cell * 0.32, false,
                    hull, cannon, engine, core, emitter, ColorMode.BLUE, time, 1f, sc);
        } else {
            g2.setFont(small);
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("なし", x + pad + (int) (14 * adj), sy + cell / 2 + (int) (4 * adj));
        }
        g2.setFont(small);
        g2.setColor(Color.WHITE);
        int tx = x + pad + cell + (int) (12 * adj);
        g2.drawString("サブ機", tx, sy + (int) (14 * adj));
        g2.setColor(equipped ? new Color(170, 230, 255) : new Color(150, 150, 150));
        g2.drawString(equipped ? SubShipService.hullLabel().replace(" ", " ") : "未装備", tx,
                sy + (int) (28 * adj));

        // --- 数値(装甲・火力・無敵・暴走) ---
        int ty = sy + cell + (int) (6 * adj);
        double hp = UnitService.baseHp(shipId) * (1 + SkillService.getBonus("MAX_HP")) * UnitService.hpRate(shipId);
        hp = hp * (1 + ModuleService.stat(ModuleStat.HP_PCT)) + ModuleService.stat(ModuleStat.HP_FLAT);
        double shield = hp * 0.15 * (1 + ModuleService.stat(ModuleStat.SHIELD_PCT))
                + ModuleService.stat(ModuleStat.SHIELD_FLAT);
        double atk = (1 + SkillService.getBonus("DAMAGE")) * UnitService.attackRate(shipId)
                * (1 + ModuleService.stat(ModuleStat.ATK_PCT));
        String[][] rows = {
                { "装甲", String.valueOf(Math.round(hp)) },
                { "盾", String.valueOf(Math.round(shield)) },
                { "火力", String.format("×%.2f", atk) },
                { "防御", String.format("-%.0f%%", ModuleService.stat(ModuleStat.DEF_PCT) * 100) },
                { "無敵", String.format("%.1f秒", 1 + SkillService.getBonus("INVINCIBLE")) },
                { "暴走", String.format("%.0f秒", 10 + SkillService.getBonus("OOC_TIME")) },
        };
        int line = (int) (13 * adj);
        for (int i = 0; i < rows.length; i++) {
            g2.setFont(small);
            g2.setColor(new Color(170, 200, 220));
            g2.drawString(rows[i][0], x + pad, ty + i * line + (int) (10 * adj));
            g2.setFont(bold);
            g2.setColor(Color.WHITE);
            g2.drawString(rows[i][1], x + pad + (int) (40 * adj), ty + i * line + (int) (10 * adj));
        }

        // --- モジュール枠(装着中のものは種類の文字とレア度の色) ---
        int my = ty + rows.length * line + (int) (4 * adj);
        g2.setFont(small);
        g2.setColor(new Color(170, 200, 220));
        g2.drawString("モジュール", x + pad, my + (int) (9 * adj));
        int slot = (int) (14 * adj);
        for (int i = 0; i < 4; i++) {
            int sx = x + pad + (int) (58 * adj) + i * (slot + (int) (4 * adj));
            Module m = ModuleService.slotted()[i];
            if (m == null) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(sx, my - 1, slot, slot, 4, 4);
                g2.setColor(new Color(255, 255, 255, 90));
                g2.drawRoundRect(sx, my - 1, slot, slot, 4, 4);
            } else {
                Color rc = CostPanel.rarityColor(m.getRarity());
                g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 90));
                g2.fillRoundRect(sx, my - 1, slot, slot, 4, 4);
                g2.setColor(rc);
                g2.drawRoundRect(sx, my - 1, slot, slot, 4, 4);
                g2.setFont(small);
                g2.setColor(Color.WHITE);
                g2.drawString(m.getKind().getShortName(), sx + (int) (2 * adj), my + (int) (10 * adj));
            }
        }
        g2.dispose();
    }
}
