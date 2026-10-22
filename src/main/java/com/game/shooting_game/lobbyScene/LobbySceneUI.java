package com.game.shooting_game.lobbyScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.armor.ArmorService;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.subship.SubShipRenderer;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.gameScene.DamageCalculator;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.gameScene.ship.types.Player;
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
        drawBars(g);
        drawTabBar(g, uiData.ships.length, uiData.ind);
        drawUserLevel(g);
        drawStats(g, uiData.ships[uiData.ind]);
        drawMessage(g);
    }

    /**
     * 上のステータスバーと下のボタン列の帯(暗い紺の板と、ネオンの水色の縁)
     * 機体の上に重ねるので、少し透かす。ボタンやレベル表示は、この上に描かれる。
     */
    private void drawBars(Graphics g) {
        com.game.shooting_game.ui.SceneBars.draw(g, com.game.shooting_game.ui.SceneBars.TOP, 104);
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
                    new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() - 340 * ShootingGameMain.getScreenSizeAdj())));
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
                MAIN.getHeight() - 270 * ShootingGameMain.getScreenSizeAdj()); // 船(と、その下の炎)にめり込まない位置
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

        double adj = ShootingGameMain.getScreenSizeAdj();
        int y = (int) (8 * adj);
        int barW = (int) (112 * adj), barH = (int) (18 * adj);
        double ratio = maxExp > 0 ? Math.clamp((double) exp / maxExp, 0.0, 1.0) : 1.0;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // レベル
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, (float) (18 * adj)));
        g2.setColor(Color.WHITE);
        String lv = "Lv." + level;
        // 画面上部、装備カードと右上のボタンの間に置く(左上のコインとも重ならないように)
        int x = (int) (160 * adj); // 左の装備カード(〜156)と右のボタン(305〜)の間に置く
        g2.drawString(lv, x, y + g2.getFontMetrics().getAscent());
        int barX = x + g2.getFontMetrics().stringWidth(lv) + (int) (8 * adj);
        int barY = y + (int) (2 * adj);
        // ゲージ(背景→中身→枠)
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
        g2.setColor(new Color(70, 215, 255));
        g2.fillRoundRect(barX, barY, (int) (barW * ratio), barH, barH, barH);
        g2.setColor(new Color(255, 255, 255, 140));
        g2.drawRoundRect(barX, barY, barW, barH, barH, barH);
        // 数値(ゲージの内側の中央。中身の色の上でも読めるよう、影をつける)
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, (float) (11 * adj)));
        java.awt.FontMetrics nfm = g2.getFontMetrics();
        String num = maxExp > 0 ? exp + " / " + maxExp : "MAX";
        int nx = barX + (barW - nfm.stringWidth(num)) / 2;
        int ny = barY + (barH + nfm.getAscent() - nfm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(num, nx + 1, ny + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(num, nx, ny);
        g2.dispose();
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

    // ----- 機体の能力(左のバー) -----

    private static final double DEFAULT_BULLET_DAMAGE = 5; // 弾の設定が読めないときの値
    private static final double STAT_BASE = 100; // 能力の基準の値(これがバーの基準の長さ)
    private static final double OVERFLOW_PER_DECADE = 0.2; // 基準を10倍超えるごとに、バーを基準の長さの何割伸ばすか

    /**
     * バーの長さの割合(基準=1)。基準までは比例し、超えた分は対数(常用対数 × OVERFLOW_PER_DECADE)で伸ばす。
     * 0以下は0。
     */
    static double barRatio(double raw) {
        if (raw <= 0)
            return 0;
        return raw <= 1 ? raw : 1 + Math.log10(raw) * OVERFLOW_PER_DECADE;
    }

    /** バーの右端が画面からはみ出さないための、バーの長さの上限(バーの左端 bx から画面の右端まで) */
    private int getBarLimit(int bx) {
        return MAIN.getWidth() - bx - (int) (4 * ShootingGameMain.getScreenSizeAdj());
    }

    /** 自機の基本の弾(STRAIGHT の1番)のダメージ */
    private double basicBulletDamage() {
        try {
            return BulletManager.getInstance().getBulletConfig().get(ShipKinds.PLAYER)
                    .get(BulletManager.BulletKinds.STRAIGHT).get(1).getDamage();
        } catch (RuntimeException e) {
            return DEFAULT_BULLET_DAMAGE;
        }
    }

    /**
     * 閲覧中の機体の能力のバー(装甲・盾・火力・防御・無敵・暴走)を、ラベル+数値+バーで描く。
     * バーの長さは目安(それぞれの値を、おおよその上限で割った割合)。
     */
    private void drawStats(Graphics g, String shipId) {
        if (DataManager.getUserData() == null)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj() * 0.8; // 能力のUIは、ふだんの0.8倍の大きさ
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double baseHp = UnitService.baseHp(shipId);
        double hp = baseHp * (1 + SkillService.getBonusFor(shipId, "MAX_HP")) * UnitService.hpRate(shipId);
        hp = hp * (1 + ModuleService.stat(ModuleStat.HP_PCT)) + ModuleService.stat(ModuleStat.HP_FLAT);
        double shield = hp * UnitService.baseShieldRate(shipId) * (1 + ModuleService.stat(ModuleStat.SHIELD_PCT))
                + ModuleService.stat(ModuleStat.SHIELD_FLAT);
        double atk = (1 + SkillService.getBonusFor(shipId, "DAMAGE")) * UnitService.attackRate(shipId)
                * (1 + ModuleService.stat(ModuleStat.ATK_PCT));
        // 火力は倍率ではなく、基本の弾1発のダメージ(倍率をかけ、実数値の攻撃力を足した値)
        double power = GameUtils.attackDamage(basicBulletDamage(), atk, ModuleService.stat(ModuleStat.ATK_FLAT));
        // 会心(閲覧中の機体のスキルで計算する。表示は%。DamageCalculator.critRate / critMult と同じ式)
        double critRate = Math.clamp(DamageCalculator.BASE_CRIT_RATE + SkillService.getBonusFor(shipId, "CRIT_RATE")
                + ModuleService.stat(ModuleStat.CRIT_RATE), 0.0, 1.0) * 100;
        double critDmg = (DamageCalculator.BASE_CRIT_MULT + SkillService.getBonusFor(shipId, "CRIT_DMG")
                + ModuleService.stat(ModuleStat.CRIT_DMG)) * 100;
        Object[][] rows = {
                { "火力", String.valueOf(Math.round(power)), power / STAT_BASE },
                { "装甲", String.valueOf(Math.round(hp)), hp / STAT_BASE },
                { "盾", String.valueOf(Math.round(shield)), shield / STAT_BASE },
                { "会心率", String.format("%.1f%%", critRate), critRate / STAT_BASE },
                { "会心威力", String.format("%.0f%%", critDmg), critDmg / STAT_BASE },
        };

        int x = (int) (10 * adj);
        int y = (int) (58 * ShootingGameMain.getScreenSizeAdj()); // 上の帯(44)の下
        int labelW = (int) (48 * adj), valueW = (int) (54 * adj);
        int barW = (int) (96 * 0.7 * adj), barH = (int) (7 * adj);
        int line = (int) (20 * adj);
        Font label = new Font("SansSerif", Font.PLAIN, Math.max(7, (int) (11 * adj)));
        Font value = new Font("SansSerif", Font.BOLD, Math.max(7, (int) (11 * adj)));

        for (int i = 0; i < rows.length; i++) {
            int ry = y + i * line;
            g2.setFont(label);
            g2.setColor(new Color(170, 200, 220));
            g2.drawString((String) rows[i][0], x, ry + (int) (9 * adj));
            g2.setFont(value);
            g2.setColor(new Color(0, 0, 0, 160)); // 背景の板がないので、文字には影をつける
            g2.drawString((String) rows[i][1], x + labelW + 1, ry + (int) (9 * adj) + 1);
            g2.setColor(Color.WHITE);
            g2.drawString((String) rows[i][1], x + labelW, ry + (int) (9 * adj));
            // バー(灰色の下地は敷かず、中身だけ)
            double raw = (double) rows[i][2];
            // すべての項目で、基準(値が100=長さ1)を超えた分を、対数で右へ伸ばす
            double ratio = barRatio(raw);
            int bx = x + labelW + valueW, by = ry + (int) (3 * adj);
            int len = (int) Math.min(barW * ratio, getBarLimit(bx));
            g2.setColor(new Color(255, 205, 40));
            g2.fillRect(bx, by, len, barH);
        }
        g2.dispose();
    }

    // ----- 装備スロット(下のカード) -----

    /** 下のカードの種類 */
    public enum Slot {
        UNIT("機体"), SUBSHIP("サブ機"), DEFENSE("防御装甲"), FIREPOWER("火力装甲");

        private final String label;

        Slot(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * 装甲のアイコン(防御は青い盾、火力は橙の刃を3本)
     *
     * @param cx   中心の x
     * @param cy   中心の y
     * @param size 全体の大きさ
     */
    private static void drawArmorIcon(Graphics2D g, boolean defense, double cx, double cy, double size) {
        double r = size / 2;
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        if (defense) {
            p.moveTo(cx - r * 0.8, cy - r * 0.85);
            p.lineTo(cx + r * 0.8, cy - r * 0.85);
            p.lineTo(cx + r * 0.8, cy + r * 0.1);
            p.quadTo(cx + r * 0.7, cy + r * 0.7, cx, cy + r);
            p.quadTo(cx - r * 0.7, cy + r * 0.7, cx - r * 0.8, cy + r * 0.1);
            p.closePath();
            g.setPaint(new java.awt.GradientPaint(0, (float) (cy - r), new Color(120, 200, 255), 0, (float) (cy + r),
                    new Color(40, 90, 200)));
            g.fill(p);
            g.setColor(new Color(220, 245, 255));
        } else {
            // 上向きの刃(中央が長く、左右が短い)
            for (int i = -1; i <= 1; i++) {
                double h = i == 0 ? 1.0 : 0.7, off = i * r * 0.62;
                java.awt.geom.Path2D b = new java.awt.geom.Path2D.Double();
                b.moveTo(cx + off, cy - r * h);
                b.lineTo(cx + off + r * 0.26, cy + r * 0.8);
                b.lineTo(cx + off - r * 0.26, cy + r * 0.8);
                b.closePath();
                p.append(b, false);
            }
            g.setPaint(new java.awt.GradientPaint(0, (float) (cy - r), new Color(255, 230, 120), 0, (float) (cy + r),
                    new Color(230, 70, 30)));
            g.fill(p);
            g.setColor(new Color(255, 245, 200));
        }
        g.setStroke(new java.awt.BasicStroke(1.4f));
        g.draw(p);
    }

    /**
     * 装備スロットのカードを描く。押せる印として、マウスを乗せると縁が光り、未解放はグレーにする。
     *
     * @param rects   カードの位置(Slot の順)
     * @param hovered マウスが乗っているか(Slot の順)
     * @param locked  未解放か(Slot の順)
     */
    public void drawSlots(Graphics g, java.awt.Rectangle[] rects, boolean[] hovered, boolean[] locked) {
        if (DataManager.getUserData() == null)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font title = new Font("SansSerif", Font.BOLD, Math.max(8, (int) (8.5 * adj)));
        Font small = new Font("SansSerif", Font.BOLD, Math.max(7, (int) (7.5 * adj)));
        String shipId = DataManager.getUserData().getSelectedShip();

        for (Slot s : Slot.values()) {
            java.awt.Rectangle r = rects[s.ordinal()];
            boolean hover = hovered[s.ordinal()], lock = locked[s.ordinal()];
            // 板(紫がかった暗い青。マウスを乗せると明るく)
            g2.setPaint(new java.awt.GradientPaint(0, r.y, hover ? new Color(80, 60, 150) : new Color(52, 38, 108),
                    0, r.y + r.height, new Color(22, 16, 56)));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g2.setColor(lock ? new Color(110, 110, 130) : (hover ? new Color(255, 235, 120) : new Color(255, 214, 80)));
            g2.setStroke(new java.awt.BasicStroke(hover ? 2.4f : 1.6f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);

            // 中身
            int pad = (int) (4 * adj);
            int ix = r.x + pad, iy = r.y + pad + (int) (3 * adj);
            int iw = r.width - 2 * pad, ih = r.height - 2 * pad - (int) (20 * adj);
            Graphics2D c = (Graphics2D) g2.create();
            c.clipRect(ix, iy, iw, ih);
            String sub = ""; // 下の小さな文字
            switch (s) {
                case UNIT -> {
                    BufferedImage[] frames = SHIP_M.getShipImage().get(shipId);
                    if (frames != null && frames.length > 0 && frames[0] != null) {
                        BufferedImage img = trimmed(shipId, frames[0]);
                        double k = Math.min((double) iw / img.getWidth(), (double) ih / img.getHeight()) * 0.95;
                        int dw = (int) (img.getWidth() * k), dh = (int) (img.getHeight() * k);
                        c.drawImage(img, ix + (iw - dw) / 2, iy + (ih - dh) / 2, dw, dh, null);
                    }
                    sub = "Lv" + UnitService.getLevel(shipId) + " " + "★".repeat(UnitService.getRank(shipId));
                }
                case SUBSHIP -> {
                    if (SubShipService.isEquipped()) {
                        var type = com.game.shooting_game.domain.subship.SubShipTypeService.current();
                        int[] look = SubShipService.look(type, SubShipService.level());
                        int typeIdx = SubShipService.typeIndex(type);
                        double sc = 0.55 * Math.min(iw, ih) / (38 * adj);
                        SubShipRenderer.draw(c, ix + iw / 2.0, iy + ih / 2.0, true, look[0], look[1], look[2],
                                look[3], look[4], typeIdx, ColorMode.BLUE, System.nanoTime() / 1e9, 1f, sc);
                        sub = SubShipService.levelLabel();
                    } else {
                        sub = "未装備";
                    }
                }
                case DEFENSE, FIREPOWER -> {
                    ArmorService.Type t = s == Slot.DEFENSE ? ArmorService.Type.DEFENSE : ArmorService.Type.FIREPOWER;
                    drawArmorIcon(c, s == Slot.DEFENSE, ix + iw / 2.0, iy + ih / 2.0, Math.min(iw, ih) * 0.9);
                    int lv = ArmorService.level(t);
                    sub = "Lv" + lv + " " + "★".repeat(ArmorService.stars(lv));
                }
            }
            c.dispose();

            // 名前(上の左)と、下の小さな文字
            g2.setFont(title);
            g2.setColor(lock ? new Color(150, 150, 165) : Color.WHITE);
            g2.drawString(s.label(), r.x + pad, r.y + pad + (int) (5 * adj));
            g2.setFont(small);
            g2.setColor(lock ? new Color(150, 150, 165) : new Color(255, 214, 80));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sub, r.x + (r.width - fm.stringWidth(sub)) / 2, r.y + r.height - (int) (5 * adj));
            if (lock) {
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            }
        }
        g2.dispose();
    }
}
