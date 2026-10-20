package com.game.shooting_game.encyclopediaScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.dex.DexService;
import com.game.shooting_game.gameScene.ship.BossLibrary;
import com.game.shooting_game.gameScene.ship.BossLibrary.BossDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipConfig;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * 敵図鑑シーン
 * 出会った敵・ボスをカードで並べ、選んだものの詳細を下に表示する
 */
public final class EncyclopediaManager extends JPanel implements Manager {
    private static final String component = "[EncyclopediaManager]";
    private static EncyclopediaManager instance;

    private final ShootingGameMain MAIN;

    private JButton goBackBtn;
    private JButton enemyTabBtn;
    private JButton bossTabBtn;
    private javax.swing.Icon enemyTabOn; // 選ばれているタブの絵
    private javax.swing.Icon enemyTabOff;
    private javax.swing.Icon bossTabOn;
    private javax.swing.Icon bossTabOff;

    private ShipKinds currentKind = ShipKinds.ENEMY; // 表示中のタブ
    private List<String> ids = new ArrayList<>(); // 表示中のタブの機体Id(設定ファイルの順)
    private int seenCount = 0; // 出会った数
    private String selectedId; // 選択中の機体Id
    private double scrollOffset = 0; // スクロール量
    private double maxScrollOffset = 0; // 最大スクロール量
    private final int col = 4; // 一行の個数

    private final Map<String, BufferedImage> bossImageCache = new HashMap<>(); // ボス画像のキャッシュ
    private final Set<String> bossImageMissing = new HashSet<>(); // 読み込めなかったボス画像

    private EncyclopediaManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    /**
     * インスタンスの取得
     *
     * @return インスタンス
     */
    public static EncyclopediaManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, EncyclopediaManager::new);
    }

    public void initComponents() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        setLayout(null);
        setOpaque(false);

        // タブボタン(タイトルの下に並べる)
        Vector2 tabSize = new Vector2(120 * adj, 30 * adj);
        int tabY = (int) (45 * adj);
        enemyTabBtn = UIBuilder.createImageBtn(component, "/static/img/UI/encyclopediaScene/enemyTab.png", tabSize, "ENEMY");
        enemyTabOn = UIBuilder.createImageBtn(component, "/static/img/UI/encyclopediaScene/enemyTab_on.png", tabSize, "ENEMY").getIcon();
        enemyTabOff = enemyTabBtn.getIcon();
        enemyTabBtn.setBounds(MAIN.getWidth() / 2 - (int) tabSize.getX() - (int) (5 * adj), tabY,
                (int) tabSize.getX(), (int) tabSize.getY());
        enemyTabBtn.addActionListener(e -> changeTab(ShipKinds.ENEMY));
        this.add(enemyTabBtn);

        bossTabBtn = UIBuilder.createImageBtn(component, "/static/img/UI/encyclopediaScene/bossTab.png", tabSize, "BOSS");
        bossTabOn = UIBuilder.createImageBtn(component, "/static/img/UI/encyclopediaScene/bossTab_on.png", tabSize, "BOSS").getIcon();
        bossTabOff = bossTabBtn.getIcon();
        bossTabBtn.setBounds(MAIN.getWidth() / 2 + (int) (5 * adj), tabY,
                (int) tabSize.getX(), (int) tabSize.getY());
        bossTabBtn.addActionListener(e -> changeTab(ShipKinds.BOSS));
        this.add(bossTabBtn);

        // 戻るボタン(一つ前の画面に遷移)
        goBackBtn = UIBuilder.createBackBtn(component, () -> SceneManager.getInstance().changeToPrevScene());
        this.add(goBackBtn);

        // マウスホイールでスクロール(グリッドの上でだけ)
        addMouseWheelListener(e -> {
            Point p = SwingUtilities.convertPoint(this, e.getPoint(), MAIN);
            if (!gridArea().contains(p))
                return;
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * 20, -maxScrollOffset, 0);
            repaint();
        });

        // クリックでカードを選択
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(EncyclopediaManager.this, e.getPoint(), MAIN);
                String hit = cardAt(p);
                if (hit != null) {
                    selectedId = hit;
                    repaint();
                }
            }
        });
    }

    @Override
    public void setup() {
        currentKind = ShipKinds.ENEMY;
        refresh();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // タイトル
        Vector2 titlePos = new Vector2(MAIN.getWidth() / 2, 10 * adj);
        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), titlePos, 24 * adj, Color.WHITE, Font.BOLD,
                currentKind.name() + " LIST  " + seenCount + "/" + ids.size());

        drawCards(g2);
        drawDetail(g2);
    }

    // ----- 状態の更新 -----
    /**
     * タブの切り替え
     */
    private void changeTab(ShipKinds kind) {
        currentKind = kind;
        refresh();
    }

    /**
     * 表示するリスト・スクロール・選択・タブの表示を作り直す
     */
    private void refresh() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        scrollOffset = 0;
        selectedId = null;

        ids = new ArrayList<>();
        Map<String, ShipDefinition> config = ShipManager.getInstance().getShipConfig().get(currentKind);
        if (config != null)
            for (Map.Entry<String, ShipDefinition> entry : config.entrySet())
                if (entry.getValue().getDatas() != null)
                    ids.add(entry.getKey());
        seenCount = DexService.seenCount(ids);

        // スクロール制限
        int rows = (ids.size() + col - 1) / col;
        maxScrollOffset = Math.max(0, rows * pitchY() + 10 * adj - gridArea().height);

        // 選択中のタブが分かるようにする(明るい絵に切り替える。画像がなければ文字で示す)
        boolean enemySel = currentKind == ShipKinds.ENEMY;
        if (enemyTabOn != null && enemyTabOff != null) {
            enemyTabBtn.setIcon(enemySel ? enemyTabOn : enemyTabOff);
            bossTabBtn.setIcon(enemySel ? bossTabOff : bossTabOn);
        } else {
            enemyTabBtn.setText(enemySel ? "[ ENEMY ]" : "ENEMY");
            bossTabBtn.setText(enemySel ? "BOSS" : "[ BOSS ]");
        }
        repaint();
    }

    // ----- レイアウト(ウィンドウ座標) -----
    /**
     * カードの縦の間隔(枠80 + 名前の分 + 余白)
     */
    private double pitchY() {
        return 110 * ShootingGameMain.getScreenSizeAdj();
    }

    /**
     * グリッド(カードを並べる領域)
     */
    private java.awt.Rectangle gridArea() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int width = (int) ((80 * col + 10 * (col - 1)) * adj);
        int top = (int) (85 * adj);
        int bottom = detailArea().y - (int) (10 * adj);
        return new java.awt.Rectangle(MAIN.getWidth() / 2 - width / 2, top, width, Math.max(0, bottom - top));
    }

    /**
     * 詳細パネルの領域(戻るボタンのすぐ上)
     */
    private java.awt.Rectangle detailArea() {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int width = (int) ((80 * col + 10 * (col - 1)) * adj);
        int height = (int) (100 * adj);
        int goBackTop = MAIN.getHeight() - (int) (50 * adj) - (int) (5 * adj);
        return new java.awt.Rectangle(MAIN.getWidth() / 2 - width / 2,
                goBackTop - (int) (10 * adj) - height, width, height);
    }

    /**
     * カードの枠の中心
     */
    private Vector2 cardCenter(int index) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Rectangle grid = gridArea();
        return new Vector2(
                grid.x + (40 + 90 * (index % col)) * adj,
                grid.y + scrollOffset + 40 * adj + pitchY() * (index / col));
    }

    /**
     * 位置にあるカードのId(なければnull)
     */
    private String cardAt(Point p) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        if (!gridArea().contains(p))
            return null;
        for (int i = 0; i < ids.size(); i++) {
            Vector2 c = cardCenter(i);
            if (Math.abs(p.x - c.getX()) <= 40 * adj && Math.abs(p.y - c.getY()) <= 40 * adj)
                return ids.get(i);
        }
        return null;
    }

    // ----- 描画 -----
    private void drawCards(Graphics2D g2) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Rectangle grid = gridArea();
        Graphics2D gc = (Graphics2D) g2.create();
        gc.setClip(grid.x, grid.y, grid.width, grid.height);

        for (int i = 0; i < ids.size(); i++) {
            Vector2 c = cardCenter(i);
            // 画面外のカードは描画しない
            if (c.getY() + 60 * adj < grid.y || c.getY() - 40 * adj > grid.y + grid.height)
                continue;

            String id = ids.get(i);
            boolean seen = DexService.isSeen(id);
            drawFrame(gc, c, 80 * adj, seen, id.equals(selectedId));

            if (seen) {
                drawSprite(gc, id, c, 80 * adj);
            } else {
                UIBuilder.drawString(gc, Anchor.CENTER, c, 36 * adj, new Color(255, 255, 255, 90), Font.BOLD, "?");
            }

            // 名前
            Vector2 namePos = new Vector2(c.getX(), c.getY() + 40 * adj + 8 * adj);
            UIBuilder.drawString(gc, new Anchor(H.CENTER, V.TOP), namePos, 11 * adj,
                    new Color(200, 200, 200), Font.PLAIN, seen ? id : "???");
        }
        gc.dispose();
    }

    /**
     * カードの枠(暗い角丸の四角にネオン色の線)
     */
    private void drawFrame(Graphics2D g2, Vector2 center, double size, boolean seen, boolean selected) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int s = (int) size;
        int x = (int) center.getX() - s / 2;
        int y = (int) center.getY() - s / 2;
        int arc = (int) (10 * adj);

        g2.setColor(new Color(255, 255, 255, seen ? 14 : 8));
        g2.fillRoundRect(x, y, s, s, arc, arc);
        g2.setStroke(new BasicStroke(Math.max(1f, (float) ((selected ? 2.5 : 1.5) * adj))));
        g2.setColor(selected ? new Color(140, 235, 255, 255)
                : new Color(70, 215, 255, seen ? 150 : 55));
        g2.drawRoundRect(x, y, s, s, arc, arc);
    }

    /**
     * 機体画像を枠の中心に収めて描画(はみ出しは枠でクリップ)
     */
    private void drawSprite(Graphics2D g2, String id, Vector2 center, double frameSize) {
        BufferedImage image = getSprite(id);
        if (image == null) {
            UIBuilder.drawString(g2, Anchor.CENTER, center, frameSize / 8, Color.WHITE, Font.PLAIN, "No Image");
            return;
        }

        Graphics2D gc = (Graphics2D) g2.create();
        gc.clip(new RoundRectangle2D.Double(center.getX() - frameSize / 2, center.getY() - frameSize / 2,
                frameSize, frameSize, 10, 10));
        if (currentKind == ShipKinds.BOSS) {
            // 400x400のコマのうち本体は幅300ほど、中心は(200,185)。本体が枠に収まるように縮める
            double size = frameSize * 0.8 * 400 / 300;
            BufferedImage resized = UIBuilder.cachedResize(image, size);
            Vector2 pos = new Vector2(center.getX(), center.getY() + (200 - 185) / 400.0 * size);
            UIBuilder.drawImage(gc, Anchor.CENTER, pos, resized);
        } else {
            UIBuilder.drawImage(gc, Anchor.CENTER, center, UIBuilder.cachedResize(image, frameSize * 3 / 4));
        }
        gc.dispose();
    }

    /**
     * 図鑑用の画像(0コマ目)の取得
     *
     * @return 画像 なければnull
     */
    private BufferedImage getSprite(String id) {
        if (currentKind == ShipKinds.BOSS) {
            if (bossImageCache.containsKey(id))
                return bossImageCache.get(id);
            if (bossImageMissing.contains(id))
                return null;
            try {
                BufferedImage sheet = UIBuilder.loadImage("/static/img/ship/boss/" + id + "_f0.png");
                BufferedImage frame = UIBuilder.cutSpriteSheet(sheet, new Vector2(4, 2), 6)[0];
                bossImageCache.put(id, frame);
                return frame;
            } catch (IOException | RuntimeException e) {
                bossImageMissing.add(id);
                return null;
            }
        }

        Map<String, BufferedImage[]> images = ShipManager.getInstance().getShipImage();
        if (images == null)
            return null;
        BufferedImage[] frames = images.get(id + "_blue");
        return frames != null && frames.length > 0 ? frames[0] : null;
    }

    /**
     * 選択中の機体の詳細
     */
    private void drawDetail(Graphics2D g2) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Rectangle area = detailArea();
        int arc = (int) (12 * adj);

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(area.x, area.y, area.width, area.height, arc, arc);
        g2.setStroke(new BasicStroke(Math.max(1f, (float) (1.5 * adj))));
        g2.setColor(new Color(70, 215, 255, 110));
        g2.drawRoundRect(area.x, area.y, area.width, area.height, arc, arc);

        Color textColor = new Color(220, 220, 220);
        if (selectedId == null) {
            UIBuilder.drawString(g2, Anchor.CENTER, new Vector2(area.getCenterX(), area.getCenterY()), 14 * adj,
                    textColor, Font.PLAIN, "カードを選ぶと詳細が見られます");
            return;
        }

        if (!DexService.isSeen(selectedId)) {
            UIBuilder.drawString(g2, Anchor.CENTER, new Vector2(area.getCenterX(), area.getCenterY() - 12 * adj),
                    24 * adj, Color.WHITE, Font.BOLD, "???");
            UIBuilder.drawString(g2, Anchor.CENTER, new Vector2(area.getCenterX(), area.getCenterY() + 14 * adj),
                    14 * adj, textColor, Font.PLAIN, "まだ出会っていない敵です");
            return;
        }

        // 左に大きな画像
        double spriteFrame = 80 * adj;
        Vector2 spriteCenter = new Vector2(area.x + 10 * adj + spriteFrame / 2, area.getCenterY());
        drawFrame(g2, spriteCenter, spriteFrame, true, false);
        drawSprite(g2, selectedId, spriteCenter, spriteFrame);

        // 右に名前とステータス
        double textX = area.x + 10 * adj + spriteFrame + 15 * adj;
        UIBuilder.drawString(g2, new Anchor(H.LEFT, V.TOP), new Vector2(textX, area.y + 10 * adj), 18 * adj,
                Color.WHITE, Font.BOLD, selectedId);

        List<String> stats = buildStats(selectedId);
        double colWidth = (area.x + area.width - 10 * adj - textX) / 2;
        for (int i = 0; i < stats.size(); i++) {
            Vector2 pos = new Vector2(textX + colWidth * (i % 2), area.y + 40 * adj + 22 * adj * (i / 2));
            UIBuilder.drawString(g2, new Anchor(H.LEFT, V.TOP), pos, 13 * adj, textColor, Font.PLAIN, stats.get(i));
        }
    }

    /**
     * ステータスの表示行
     */
    private List<String> buildStats(String id) {
        List<String> stats = new ArrayList<>();
        if (currentKind == ShipKinds.BOSS) {
            BossDefinition boss = BossLibrary.get(id);
            stats.add("ゲージ数  " + boss.getGauges().size());
            stats.add("ゲージHP  " + (long) boss.getGaugeHp());
            return stats;
        }

        ShipDefinition def = ShipManager.getInstance().getShipConfig().get(ShipKinds.ENEMY).get(id);
        ShipConfig data = def != null ? def.getDatas() : null;
        if (data == null)
            return stats;
        stats.add("HP  " + (long) data.getHp());
        stats.add("スコア  " + (long) data.getScore());
        stats.add("コイン  " + (data.getCoins() != null ? data.getCoins() : "-"));
        stats.add("速度  " + data.getSpeed());
        return stats;
    }
}
