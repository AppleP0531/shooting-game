package src.utils;

import src.ShootingGameMain;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import javax.imageio.ImageIO;

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
            // 画像のリサイズ(少しづつ調整して画質を保つ)
            while (true) {
                if (imageSize < sub.getWidth() / 2) {
                    sub = resizeImage(sub, sub.getWidth() / 2);
                } else {
                    frames[i] = resizeImage(sub, imageSize);
                    break;
                }
            }
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
    public static JButton createImageButton(String component, String path, Vector2 size, String fallbackText) {
        JButton button;
        try {
            // 通常アイコンの登録
            BufferedImage img = loadImage(path);
            button = createImageButton(component, img, size, fallbackText);

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
    public static JButton createImageButton(String component, BufferedImage image, Vector2 size, String fallbackText) {
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
     * @param button   ボタン
     * @param basePath 元画像のパス
     * @param suffix   状態画像のサフィックス
     * @param size     画像サイズ
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
     * 各状態のボタン画像パスを作成
     * 
     * @param path 元画像のパス
     * @return 各状態のボタン画像パス
     */
    private static String createPathWithSuffix(String path, String suffix) {
        int dotIndex = path.lastIndexOf('.');
        return path.substring(0, dotIndex) + suffix + path.substring(dotIndex);
    }

    /**
     * 中央ぞろえテキストの描画
     * 
     * @param g
     * @param pos   位置
     * @param size  大きさ
     * @param color 色
     * @param style テキストスタイル(Font.BOLD,Font.PLAIN)
     * @param text  テキスト
     */
    public static void drawString(Graphics g, Vector2 pos, double size, Color color, int style, String text) {
        // フォントとサイズ
        Font font = new Font("SansSerif", style, (int) size);
        g.setFont(font);
        g.setColor(color);

        // 中央ぞろえ
        FontMetrics fm = g.getFontMetrics();
        int x = (int) (pos.getX() - fm.stringWidth(text) / 2);
        int y = (int) (pos.getY() + fm.getHeight() / 2);

        g.drawString(text, x, y);
    }

    /**
     * パネル
     * 
     * @param p     パネル
     * @param title タイトル
     * @param focus フォーカスの有無
     */
    public static void openPanel(JPanel p, String title, Boolean focus) {
        JPanel parent = (JPanel) p.getParent();
        ShootingGameMain MAIN = ShootingGameMain.getInstance();
        // パネル外のフォーカスを奪うシールドの生成
        if (focus) {
            JPanel shield = (JPanel) findComponentByName(parent, "globalShield");
            if (shield == null) {
                shield = new JPanel();
                shield.setName("globalShield");
                shield.setBounds(0, 0, MAIN.getWidth(), MAIN.getHeight());
                shield.setOpaque(false);

                MouseAdapter blocker = new MouseAdapter() {
                };
                shield.addMouseListener(blocker);
                shield.addMouseMotionListener(blocker);

                parent.add(shield);
            }

            // フォーカスを奪うシールドを敷く
            shield.setVisible(true);
            parent.setComponentZOrder(shield, 0);
        }

        // パネルのフォーカスを奪う
        JPanel panelFocus = (JPanel) findComponentByName(parent, "panelFocus");
        if (panelFocus == null) {
            panelFocus = new JPanel();
            panelFocus.setName("panelFocus");
            panelFocus.setBounds(p.getX(), p.getY(), p.getWidth(), p.getHeight());
            panelFocus.setOpaque(false);

            MouseAdapter blocker = new MouseAdapter() {
            };
            panelFocus.addMouseListener(blocker);
            panelFocus.addMouseMotionListener(blocker);

            parent.add(panelFocus);
        }
        panelFocus.setVisible(true);
        parent.setComponentZOrder(panelFocus, 0);

        // 最前面に配置
        parent.setComponentZOrder(p, 0);

        p.setLayout(null);
        p.setBackground(new Color(0, 0, 0, 220));
        p.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));

        // パネルタイトル
        JLabel titleLabel = (JLabel) findComponentByName(p, "titleLabel");
        if (titleLabel == null) {
            titleLabel = new JLabel();
            titleLabel.setForeground(Color.WHITE); // 文字色
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER); // 中央揃え

            titleLabel.setName("titleLabel");
            p.add(titleLabel);
        }
        double fontSize = 15 * ShootingGameMain.getScreenSizeAdj();
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) fontSize));
        titleLabel.setText(title);
        titleLabel.setBounds(0, 0, p.getWidth(), (int) (fontSize * 1.5));

        // closeボタン
        JButton closePanelButton = (JButton) findComponentByName(p, "closeBtn");
        if (closePanelButton == null) {
            Vector2 closePanelButtonSize = new Vector2(
                    20 * ShootingGameMain.getScreenSizeAdj(),
                    20 * ShootingGameMain.getScreenSizeAdj());
            String closePanelButtonImagePath = "/static/img/UI/closeImage.png";
            closePanelButton = UIBuilder.createImageButton("[UIBuilder]", closePanelButtonImagePath,
                    closePanelButtonSize, "X");
            closePanelButton.addActionListener(e -> {
                UIBuilder.closePanel(p);
            });

            closePanelButton.setName("closeBtn");
            p.add(closePanelButton);
        }
        closePanelButton.setBounds(
                (int) p.getWidth() - (int) closePanelButton.getWidth(),
                0,
                (int) closePanelButton.getWidth(),
                (int) closePanelButton.getHeight());

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
}
