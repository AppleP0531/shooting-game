package com.game.shooting_game.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.utils.Anchor.H;
import com.game.shooting_game.utils.Anchor.V;
import com.game.shooting_game.utils.animation.AnimationManager;

public class UIBuilder extends JPanel {
    /**
     * 画像の読み込み
     * 
     * @param component 呼び出しクラス名
     * @param path      画像パス
     * @return 読み込んだ画像 読み込めなかった場合はnull
     * @throws IOException
     */
    public static BufferedImage loadImage(String path) throws IOException {
        try (InputStream is = UIBuilder.class.getResourceAsStream(path)) {
            // ファイルが存在しなかった場合例外を投げる
            if (is == null) {
                throw new IOException("FILE_NOT_FOUND");
            }

            BufferedImage img = ImageIO.read(is);

            // 画像が壊れている場合
            if (img == null) {
                throw new IOException("BROKEN_FILE");
            }

            return img;
        }
    }

    /**
     * マスターシートの切り分け
     * 
     * @param masterSheet マスターシート
     * @param grids       一辺当たりのコマ数
     * @param useCount    使用コマ数
     * @param imageSize   最終画像サイズ
     * @return 各フレーム用に切り分けられた画像
     */
    public static BufferedImage[] cutSpriteSheet(BufferedImage masterSheet, Vector2 grids, int useCount,
            double imageSize) {
        // 1コマ当たりのサイズ
        int sheetWidth = masterSheet.getWidth() / (int) grids.getX();
        int sheetHeiht = masterSheet.getHeight() / (int) grids.getY();
        // 使用枚数分の配列
        BufferedImage[] frames = new BufferedImage[useCount];

        for (int i = 0; i < useCount; i++) {
            // 切り取り位置の計算
            int x = (i % (int) grids.getX()) * sheetWidth;
            int y = (i / (int) grids.getX()) * sheetHeiht;

            // 画像切り分け
            BufferedImage sub = masterSheet.getSubimage(x, y, sheetWidth, sheetHeiht);
            // 画像のリサイズ
            frames[i] = resizeImage(sub, imageSize);
        }

        return frames;
    }

    /**
     * 画像付きボタンの生成(パスから)
     * 
     * @param component    呼び出しクラス名
     * @param path         画像パス
     * @param size         サイズ
     * @param fallbackText 画像が読み込めなかった場合のボタンのテキスト
     * @return パスから生成されたリサイズ済みボタン
     */
    public static JButton createImageBtn(String component, String path, Vector2 size, String fallbackText) {
        JButton button;
        try {
            // 通常アイコンの登録
            BufferedImage img = loadImage(path);
            button = createImageBtn(component, img, size, fallbackText);

            // 特殊状態の画像の登録
            applyStateIcon(component, button, path, "_rollover", size);
            applyStateIcon(component, button, path, "_pressed", size);
        } catch (IOException e) {
            switch (e.getMessage()) {
                // 画像ファイル未発見ログ
                case "FILE_NOT_FOUND" -> LogUtils.systemLog(
                        "[WARN]",
                        component,
                        "File not found: " + ANSI.YELLOW + path + ANSI.RESET + ".");

                // 画像読み込みエラーログ
                case "BROKEN_FILE" -> LogUtils.systemLog(
                        "[ERROR]",
                        component,
                        "Failed to load image: " + ANSI.YELLOW + path + ANSI.RESET + ".");
                default -> System.out.println(e);
            }
            // 画像が読み込めなかった場合はテキストを挿入
            button = new JButton(fallbackText);
        }

        button.setSize((int) size.getX(), (int) size.getY());
        return button;
    }

    /**
     * 画像付きボタンの生成(画像から)
     * 
     * @param component    呼び出しクラス名
     * @param image        画像
     * @param size         サイズ
     * @param fallbackText 画像が読み込めなかった場合のボタンのテキスト
     * @return 画像から生成されたリサイズ済みボタン
     */
    public static JButton createImageBtn(String component, BufferedImage image, Vector2 size, String fallbackText) {
        JButton button;
        // 画像が与えられた場合は画像を挿入、与えられなかった場合はテキストを挿入
        if (image != null) {
            // リサイズした BufferedImage を作成
            BufferedImage scaledBI = new BufferedImage((int) size.getX(), (int) size.getY(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaledBI.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, 0, 0, (int) size.getX(), (int) size.getY(), null);
            g2.dispose();

            // 透明部分をクリック不可にするボタンの生成
            button = new JButton(new ImageIcon(scaledBI)) {
                @Override
                public boolean contains(int x, int y) {
                    // 範囲外なら無視
                    if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())
                        return false;

                    // クリックされた位置のピクセルのアルファ値の確認 (透明なら無視)
                    int pixel = scaledBI.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xff;
                    return alpha > 0;
                }
            };

            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
        } else
            button = new JButton(fallbackText);

        // サイズの設定
        button.setSize((int) size.getX(), (int) size.getY());
        return button;
    }

    /**
     * 画像のリサイズ
     * 
     * @param originImage 元画像
     * @param imageSize   画像サイズ
     * @return リサイズ後の画像
     */
    public static BufferedImage resizeImage(BufferedImage originImage, double imageSize) {
        // 画像のリサイズ(再帰呼び出しにより少しづつ調整して画質を保つ)
        while (imageSize < originImage.getWidth() / 2) {
            originImage = resizeImage(originImage, originImage.getWidth() / 2);
        }

        // 元の画像サイズ
        Vector2 originSize = new Vector2(originImage.getWidth(), originImage.getHeight());
        // 倍率計算
        double scale = imageSize / Math.max(originSize.getX(), originSize.getY());
        // 調整後の画像サイズ
        Vector2 size = new Vector2(originSize.getX() * scale, originSize.getY() * scale);

        // 調整後の画像
        BufferedImage image = new BufferedImage(
                (int) size.getX(),
                (int) size.getY(),
                BufferedImage.TYPE_INT_ARGB);

        // 画像の書き込み
        Graphics2D g2 = image.createGraphics();

        applyHighQualityRenderSettings(g2);

        g2.drawImage(originImage, 0, 0, (int) size.getX(), (int) size.getY(), null);
        g2.dispose();

        return image;
    }

    public static void applyHighQualityRenderSettings(Graphics2D g2) {
        // --- 画質向上のためのレンダリングヒント設定 ---
        // レンダリング品質：最高品質を指定
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // アンチエイリアス：有効化
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 4. ストローク制御：ピクセル境界を正確に扱う
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * 状態画像の適用
     * 
     * @param component 呼び出し元クラス名
     * @param button    ボタン
     * @param basePath  元画像のパス
     * @param suffix    状態画像のサフィックス
     * @param size      画像サイズ
     */
    private static void applyStateIcon(String component, JButton button, String basePath, String suffix, Vector2 size) {
        // 状態画像のパス作成
        String statePath = createPathWithSuffix(basePath, suffix);
        try {
            BufferedImage img = loadImage(statePath);

            // リサイズ処理
            BufferedImage scaledBI = new BufferedImage((int) size.getX(), (int) size.getY(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaledBI.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, 0, 0, (int) size.getX(), (int) size.getY(), null);
            g2.dispose();

            ImageIcon icon = new ImageIcon(scaledBI);

            // 画像のセット
            if (suffix.equals("_rollover")) {
                button.setRolloverIcon(icon);
                button.setRolloverEnabled(true);
            } else if (suffix.equals("_pressed")) {
                button.setPressedIcon(icon);
            }
        } catch (IOException e) {
            // 画像ファイル未発見ログ
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    "File not found: " + ANSI.YELLOW + statePath + ANSI.RESET + ".");
        }
    }

    /**
     * テキストの描画
     * 
     * @param g
     * @param anchor 描画の基準点
     * @param pos    位置
     * @param size   大きさ
     * @param color  色
     * @param style  テキストスタイル(Font.BOLD,Font.PLAIN)
     * @param text   テキスト
     */
    public static void drawString(Graphics g, Anchor anchor, Vector2 pos, double size, Color color, int style,
            String text) {
        // フォントとサイズ
        Font font = new Font("SansSerif", style, (int) size);
        g.setFont(font);
        g.setColor(color);

        // 基準点に基づいて配置
        FontMetrics fm = g.getFontMetrics();
        int x = (int) (pos.getX() - fm.stringWidth(text) * anchor.getValue().getX());
        int y = (int) (pos.getY() + (fm.getAscent() - fm.getHeight() * anchor.getValue().getY()));

        g.drawString(text, x, y);
    }

    /**
     * 画像の描画
     * 
     * @param g
     * @param anchor 描画の基準点
     * @param pos    位置
     * @param image  画像
     */
    public static void drawImage(Graphics g, Anchor anchor, Vector2 pos, BufferedImage image) {
        int x = (int) (pos.getX() - image.getWidth() * anchor.getValue().getX());
        int y = (int) (pos.getY() - image.getHeight() * anchor.getValue().getY());
        g.drawImage(image, x, y, image.getWidth(), image.getHeight(),
                null);
    }

    @FunctionalInterface
    public interface CloseBtnAction {
        void onClick();
    }

    /**
     * パネル (タイトル表示のために上部分に25px相当のパネルが張り付けられます)
     * 
     * @param p           パネル
     * @param title       タイトル
     * @param focus       フォーカスの有無
     * @param closeAction クローズボタンのアクション
     */
    public static void openPanel(JPanel p, String title, Boolean focus, CloseBtnAction closeAction) {
        p.setLayout(null);
        p.setBackground(new Color(0, 0, 0, 220));
        p.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));

        JPanel parent = (JPanel) p.getParent();
        ShootingGameMain MAIN = ShootingGameMain.getInstance();

        // タイトルパネル
        double titlePanelSize = 25 * ShootingGameMain.getScreenSizeAdj();
        JPanel titlePanel = (JPanel) findComponentByName(parent, "titlePanel");
        if (titlePanel == null) {
            titlePanel = new JPanel();
            titlePanel.setLayout(null);
            titlePanel.setBackground(new Color(0, 0, 0, 220));
            titlePanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            titlePanel.setName("titlePanel");
            titlePanel.setBounds(p.getX(), p.getY() - (int) titlePanelSize,
                    p.getWidth(), (int) titlePanelSize);
            parent.add(titlePanel);
        }
        titlePanel.setVisible(true);

        if (focus) {
            // 画面全体のフォーカスを奪うシールドの生成
            JPanel shield = (JPanel) findComponentByName(parent, "globalShield");
            if (shield == null) {
                shield = new JPanel();
                shield.setName("globalShield");
                shield.setBounds(0, 0, MAIN.getWidth(), MAIN.getHeight());
                shield.setOpaque(false);

                mouseFocus(shield);

                parent.add(shield);
            }
            shield.setVisible(true);
            parent.setComponentZOrder(shield, 0);
        } else {
            // パネルのフォーカスを奪う
            JPanel panelFocus = (JPanel) findComponentByName(parent, "panelFocus");
            if (panelFocus == null) {
                panelFocus = new JPanel();
                panelFocus.setName("panelFocus");
                panelFocus.setBounds(
                        p.getX(),
                        p.getY() - (int) titlePanelSize,
                        p.getWidth(),
                        p.getHeight() + (int) titlePanelSize);
                panelFocus.setOpaque(false);

                mouseFocus(panelFocus);

                parent.add(panelFocus);
            }
            panelFocus.setVisible(true);
            parent.setComponentZOrder(panelFocus, 0);
        }

        // パネルタイトル
        JLabel titleLabel = (JLabel) findComponentByName(titlePanel, "titleLabel");
        if (titleLabel == null) {
            titleLabel = new JLabel();
            titleLabel.setForeground(Color.WHITE); // 文字色
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER); // 中央揃え

            titleLabel.setName("titleLabel");
            titlePanel.add(titleLabel);
        }
        double fontSize = 15 * ShootingGameMain.getScreenSizeAdj();
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) fontSize));
        titleLabel.setText(title);
        titleLabel.setBounds(0, 0, titlePanel.getWidth(), (int) titlePanelSize);

        // closeボタン
        JButton closePanelBtn = (JButton) findComponentByName(titlePanel, "closeBtn");
        if (closePanelBtn == null) {
            Vector2 closePanelBtnSize = new Vector2(
                    20 * ShootingGameMain.getScreenSizeAdj(),
                    20 * ShootingGameMain.getScreenSizeAdj());
            String closePanelBtnImagePath = "/static/img/UI/closeImage.png";
            closePanelBtn = UIBuilder.createImageBtn("[UIBuilder]", closePanelBtnImagePath,
                    closePanelBtnSize, "X");
            closePanelBtn.addActionListener(e -> {
                // クローズアクションが指定されていればそれを実行、指定されていなければパネルを閉じる
                if (closeAction != null)
                    closeAction.onClick();
                else
                    UIBuilder.closePanel(p);
            });

            closePanelBtn.setName("closeBtn");
            titlePanel.add(closePanelBtn);
        }
        closePanelBtn.setBounds(
                (int) titlePanel.getWidth() - (int) closePanelBtn.getWidth(),
                (int) titlePanelSize / 2 - (int) closePanelBtn.getHeight() / 2,
                (int) closePanelBtn.getWidth(),
                (int) closePanelBtn.getHeight());

        // 最前面に配置
        parent.setComponentZOrder(p, 0);
        parent.setComponentZOrder(titlePanel, 0);

        p.revalidate();
        p.repaint();
        p.setVisible(true);
    }

    /**
     * パネルを閉じる
     * 
     * @param p パネル情報
     */
    public static void closePanel(JPanel p) {
        Component titlePanel = findComponentByName((JPanel) p.getParent(), "titlePanel");
        if (titlePanel != null) {
            titlePanel.setVisible(false);
        }

        Component shield = findComponentByName((JPanel) p.getParent(), "globalShield");
        if (shield != null) {
            shield.setVisible(false);
        }

        Component blocker = findComponentByName((JPanel) p.getParent(), "panelFocus");
        if (blocker != null) {
            blocker.setVisible(false);
        }

        p.setVisible(false);
    }

    /**
     * 名前からコンポーネントを検索するヘルパーメソッド
     * 
     * @param panel 検索対象のパネル
     * @param name  検索するコンポーネントの名前
     * @return
     */
    private static Component findComponentByName(JPanel panel, String name) {
        if (panel == null)
            return null;

        for (Component comp : panel.getComponents()) {
            if (comp.getName() != null && comp.getName().equals(name)) {
                return comp;
            }
        }
        return null;
    }

    /**
     * マウスフォーカスを奪う
     * 
     * @param p フォーカスを奪うパネル
     */
    private static void mouseFocus(JPanel p) {
        MouseAdapter blocker = new MouseAdapter() {
        };
        p.addMouseListener(blocker);
        p.addMouseMotionListener(blocker);
        p.addMouseWheelListener(blocker);
    }

    /**
     * 与えられたリソースリストをEnumの定義順に並べ替える
     * 
     * @param resources 元のリスト
     * @return 並べ替え後のリスト
     */
    public static LinkedHashMap<ResourceKinds, Integer> sortItemList(Map<ResourceKinds, Integer> resources) {
        LinkedHashMap<ResourceKinds, Integer> sortedMap = new LinkedHashMap<>();
        resources.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    // 内部のEnumを取り出して比較する
                    ResourceKinds.ResourceName name1 = entry1.getKey().getResName();
                    ResourceKinds.ResourceName name2 = entry2.getKey().getResName();

                    // アイテム名で比較
                    int nameCompare = Integer.compare(name1.ordinal(), name2.ordinal());
                    if (nameCompare != 0) {
                        return nameCompare;
                    }

                    // レアリティで比較
                    ResourceKinds.Rarity rarity1 = entry1.getKey().getRarity();
                    ResourceKinds.Rarity rarity2 = entry2.getKey().getRarity();
                    return Integer.compare(rarity1.ordinal(), rarity2.ordinal());
                })
                .forEachOrdered(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        return sortedMap;
    }

    /**
     * 所有資源の各々の項目の描画
     * 
     * @param g
     * @param pos     位置
     * @param image   画像
     * @param resKind 名前
     * @param amount  個数
     */
    public static void drawEquipmentsIcon(Component c, Graphics g, Vector2 pos, Vector2 size, String uuid,
            ResourceKinds resKind, int amount) {
        Graphics2D g2 = (Graphics2D) g;

        // コンポーネント座標をウィンドウ座標に変換
        Point p = new Point((int) pos.getX(), (int) pos.getY());
        Point windowPos = SwingUtilities.convertPoint(c, p, ShootingGameMain.getInstance());
        pos = new Vector2(windowPos.x, windowPos.y);

        Vector2 imageSize = new Vector2(
                size.getX() * 3 / 4,
                size.getY() * 3 / 4);

        // 枠の描画
        BufferedImage[] frameImageFrames = null;
        // 画像情報の取得
        Map<ResourceName, Map<Rarity, BufferedImage[]>> viewCache = ResourceManager.getInstance().getResourceImage();
        if (viewCache != null) {
            Map<Rarity, BufferedImage[]> rarityMap = viewCache.get(ResourceName.FRAME);
            if (rarityMap != null)
                frameImageFrames = rarityMap.get(resKind.getRarity());
        }
        if (frameImageFrames != null && frameImageFrames.length > 0) {
            BufferedImage frameImage = frameImageFrames[AnimationManager.getInstance().getCurrentFrameNum(uuid)];
            frameImage = resizeImage(frameImage, size.getX());
            drawImage(g2, Anchor.CENTER, pos, frameImage);
        } else {
            g2.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
            g2.drawRect((int) (pos.getX() - imageSize.getX() / 2),
                    (int) (pos.getY() - imageSize.getY() / 2),
                    (int) imageSize.getX(),
                    (int) imageSize.getY());
        }

        // 画像の描画
        BufferedImage image = null;
        if (uuid != null)
            image = AnimationManager.getInstance().getCurrentImage(uuid);

        if (image != null) {
            image = resizeImage(image, size.getX() * 2 / 3);
            drawImage(g2, Anchor.CENTER, pos, image);
        } else {
            // noImageテキスト
            double textSize = size.getY() / 10;
            drawString(g, new Anchor(Anchor.CENTER), pos, textSize, Color.WHITE, Font.PLAIN,
                    "No Image");
        }

        double spacing = size.getY() / 20;

        double resNameTextSize = size.getY() / 10;
        Vector2 resNameTextPos = new Vector2(
                pos.getX(),
                pos.getY() + imageSize.getY() / 2 + spacing);
        drawString(g, new Anchor(H.CENTER, V.TOP), resNameTextPos, resNameTextSize, Color.WHITE,
                Font.PLAIN, resKind.getResName().toString());

        double amountTextSize = size.getY() / 10;
        Vector2 amountTextPos = new Vector2(
                resNameTextPos.getX(),
                resNameTextPos.getY() + resNameTextSize / 2 + spacing);
        drawString(g, new Anchor(H.CENTER, V.TOP), amountTextPos, amountTextSize, Color.WHITE,
                Font.PLAIN, String.valueOf(amount));
    }

    /**
     * パスの間に接尾辞を差し込む
     * 
     * @param path   パス
     * @param suffix 接尾辞
     * @return
     */
    public static String createPathWithSuffix(String path, String suffix) {
        int dotIndex = path.lastIndexOf('.');
        return path.substring(0, dotIndex) + suffix + path.substring(dotIndex);
    }
}
