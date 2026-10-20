package com.game.shooting_game.lobbyScene;

import com.game.shooting_game.ui.ModuleIcons;
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
import com.game.shooting_game.domain.module.Module;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.domain.subship.SubShipRenderer;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class LobbySceneUI {
    private final ShootingGameMain MAIN;
    private final GameManager GM;
    private final BulletManager BM;
    private final ShipManager SHIP_M;

    private final java.util.Map<String, BufferedImage> trimmedImages = new java.util.HashMap<>(); // 余白を除いた機体画像
    private String latestViewingShipName;
    private double levelUpInterval; // 強化の間隔
    private double levelUpIntervalTimer; // タイマー

    private static final double MESSAGE_DURATION = 2.5; // 通知を出し続ける秒数
    private String message; // 画面に出す通知(なければnull)
    private double messageTimer; // 通知の残り時間

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
        message = null;
    }

    /**
     * 更新
     * 
     * @param dt
     */
    public void update(double dt) {
        if (message != null) {
            messageTimer -= dt;
            if (messageTimer <= 0)
                message = null;
        }

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
        drawMessage(g);
    }

    /**
     * 画面中央に通知を出す(一定時間で消える)
     *
     * @param text 表示する文章
     */
    public void showMessage(String text) {
        message = text;
        messageTimer = MESSAGE_DURATION;
    }

    /**
     * 通知の描画(最後の0.5秒でフェードアウト)
     */
    private void drawMessage(Graphics g) {
        if (message == null)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        float alpha = (float) Math.min(1.0, messageTimer / 0.5);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (14 * adj))));
        int textW = g2.getFontMetrics().stringWidth(message);
        int padX = (int) (14 * adj);
        int boxH = (int) (32 * adj);
        int boxW = textW + 2 * padX;
        int bx = MAIN.getWidth() / 2 - boxW / 2;
        int by = (int) (MAIN.getHeight() * 0.62);

        g2.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
        g2.fillRoundRect(bx, by, boxW, boxH, 12, 12);
        g2.setColor(new Color(255, 120, 120, (int) (230 * alpha)));
        g2.drawRoundRect(bx, by, boxW, boxH, 12, 12);
        g2.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
        g2.drawString(message, bx + padX, by + boxH / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
        g2.dispose();
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
     * 透明な余白を除いた機体画像(アイコンに大きく収めるため)
     */
    private BufferedImage trimmed(String shipId, BufferedImage src) {
        return trimmedImages.computeIfAbsent(shipId, k -> {
            int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
            for (int yy = 0; yy < src.getHeight(); yy++) {
                for (int xx = 0; xx < src.getWidth(); xx++) {
                    if ((src.getRGB(xx, yy) >>> 24) > 24) {
                        minX = Math.min(minX, xx);
                        minY = Math.min(minY, yy);
                        maxX = Math.max(maxX, xx);
                        maxY = Math.max(maxY, yy);
                    }
                }
            }
            if (maxX < minX || maxY < minY)
                return src;
            return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        });
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
        int h = (int) (226 * adj);
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
            BufferedImage img = trimmed(shipId, frames[0]);
            double k = Math.min((double) icon / img.getWidth(), (double) icon / img.getHeight()) * 0.92;
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
        int cell = icon; // 自機のアイコンと同じ大きさにそろえる
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(x + pad, sy, cell, cell, 8, 8);
        double time = System.nanoTime() / 1e9;
        boolean equipped = SubShipService.isEquipped() && SubShipService.hullTier() > 0;
        if (equipped) {
            int hull = SubShipService.hullTier();
            int cannon = SubShipService.getTier("CANNON");
            int engine = SubShipService.getTier("ENGINE");
            int core = SubShipService.getTier("CORE");
            int emitter = SubShipService.getTier("EMITTER");
            double sc = 0.30 * cell / (38 * adj); // 枠の中に収まる大きさ
            // 左機は左下、右機は右上(枠の外には描かない)
            Graphics2D clipped = (Graphics2D) g2.create();
            clipped.clipRect(x + pad, sy, cell, cell);
            SubShipRenderer.draw(clipped, x + pad + cell * 0.30, sy + cell * 0.68, true,
                    hull, cannon, engine, core, emitter, ColorMode.BLUE, time, 1f, sc);
            SubShipRenderer.draw(clipped, x + pad + cell * 0.30 + cell * 0.45, sy + cell * 0.32, false,
                    hull, cannon, engine, core, emitter, ColorMode.BLUE, time, 1f, sc);
            clipped.dispose();
        } else {
            g2.setFont(small);
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("なし", x + pad + (int) (10 * adj), sy + cell / 2 + (int) (4 * adj));
        }
        g2.setColor(new Color(255, 214, 80, 200));
        g2.drawRoundRect(x + pad, sy, cell, cell, 8, 8);
        g2.setFont(small);
        g2.setColor(Color.WHITE);
        int tx = x + pad + cell + (int) (6 * adj);
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
                ModuleIcons.draw(g2, m, sx, my - 1, slot);
            }
        }
        g2.dispose();
    }
}
