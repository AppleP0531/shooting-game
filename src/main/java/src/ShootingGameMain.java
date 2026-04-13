package src;

import src.SceneManager.SceneType;
import src.data.DataManager;
import src.data.LoginDialog;
import src.data.UserService;
import src.utils.ANSI;
import src.utils.LogUtils;
import src.interfaces.Manager;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.AbstractAction;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import lombok.Getter;
import lombok.Setter;

@SpringBootApplication
public final class ShootingGameMain extends JPanel implements Runnable {
    private static final String component = "[MAIN]";
    private static ShootingGameMain instance;
    @Getter
    private static CommandGlassPane glassPane;

    private final CardLayout CARDLAYOUT = new CardLayout();

    private SceneType lastScene = null;
    @Getter
    private static double screenSizeAdj = 1;
    @Getter
    @Setter
    private int fps = 120; // FPS
    private Thread gameThread;
    @Setter
    private boolean loaded = false; // 初期設定が完了したかどうか
    private boolean running = false; // 処理するか

    private ShootingGameMain() {
        ShootingGameMain.instance = this;

        // PCの画面比率に基づいた調整値を設定
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSizeAdj = Math.min(screenSize.getWidth() / 1493, screenSize.getHeight() / 933);

        setPreferredSize(new Dimension(
                (int) (405 * ShootingGameMain.getScreenSizeAdj()),
                (int) (720 * ShootingGameMain.getScreenSizeAdj())));
        setBackground(Color.black);
        setFocusable(true);
        requestFocusInWindow();

        setLayout(CARDLAYOUT);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static ShootingGameMain getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, ShootingGameMain::new);
    }

    public static void setup(String[] args) {
        // ターミナルのクリア
        try {
            if (System.getProperty("os.name").contains("Windows")) { // windows用
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else { // Mac,Linux用
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            LogUtils.systemLog(
                    "[WARN]",
                    component,
                    ANSI.YELLOW + "Fail to reset logs" + ANSI.RESET);
        }

        // サーバー接続開始ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.GREEN + "Connecting to game server..." + ANSI.RESET);

        // Spring Bootの起動
        ConfigurableApplicationContext context = SpringApplication.run(ShootingGameMain.class, args);

        // サーバー接続成功ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.GREEN + "Connected to game server successfully." + ANSI.RESET);

        // 初期設定開始ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                ANSI.GREEN + "Initializing Game Setup..." + ANSI.RESET);

        // DataManagerをSpringから取得して初期化
        context.getBean(DataManager.class);

        JFrame frame = new JFrame("ShootingGame");
        ShootingGameMain panel = new ShootingGameMain();
        frame.add(panel);
        frame.pack();

        frame.setTitle("ShootingGame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.toFront(); // ウィンドウを最前面に移動

        panel.start();

        new Thread(() -> {
            // GlassPaneの設定
            glassPane = new CommandGlassPane(() -> {
            });
            frame.setGlassPane((glassPane));

            // スラッシュでコマンドウィンドウを起動できるようにする
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('/'), "openCmd");
            panel.getActionMap().put("openCmd", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    glassPane.showCommandBar();
                }
            });

            // リソース情報の読み込み
            ResourceManager.getInstance();

            // UIのセットアップ
            SceneManager.getInstance().getSceneMap().values().forEach(Manager::initComponents);

            // すべてのマネージャーをSceneManagerから取得しカードレイアウトに登録
            SceneManager.getInstance().getSceneMap().forEach((type, manager) -> {
                if (manager instanceof JPanel p) {
                    panel.add(p, type.name());
                }
            });

            panel.setLoaded(true);

            UserService userService = context.getBean(UserService.class);
            LoginDialog loginDialog = new LoginDialog(frame, userService);
            loginDialog.setVisible(true);

            // 初期設定完了ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.GREEN + "Game configuration loaded successfully." + ANSI.RESET);
            // 起動ログ
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.GREEN + "Game launching." + ANSI.RESET);

            // 最初の画面をTITLEに設定
            SceneManager.getInstance().changeScene(SceneType.TITLE);
        }).start();

        // ウィンドウを閉じたときにデータをセーブ
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // データセーブ開始ログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        ANSI.GREEN + "Saving game data..." + ANSI.RESET);

                // データ保存
                DataManager.save();

                // Goodbyeログ
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        ANSI.GREEN + "Goodbye!" + ANSI.RESET);

                System.exit(0);
            }
        });
    }

    /**
     * 処理開始
     */
    private void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    /**
     * 実行
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double deltaTime;

        while (running) {
            try {
                long now = System.nanoTime();
                double nsPerFrame = 1_000_000_000.0 / fps;

                // 秒単位に変換
                deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                if (loaded)
                    update(deltaTime);

                repaint();

                // FPS制御（目標フレーム間隔までスリープ）
                long sleepTime = (long) (nsPerFrame - (System.nanoTime() - now)) / 1_000_000;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                // ゲームループエラー
                LogUtils.systemLog(
                        "[ERROR]",
                        component,
                        "Game Loop Error.",
                        e);
            }
        }
    }

    /**
     * 更新
     * 
     * @param dt
     */
    private void update(double dt) {
        SceneType current = SceneManager.getInstance().getSceneName();
        // シーンが切り替わった時にシーン遷移処理
        if (current != lastScene) {
            CARDLAYOUT.show(this, current.name());
            lastScene = current;
        }

        SceneManager.getInstance().update(dt);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!loaded) {
            // ロゴ表示
            g.setColor(Color.WHITE);
            g.drawString("Loading...", (int) (150 * screenSizeAdj), (int) (360 * screenSizeAdj));
        } else {
            SceneManager.getInstance().draw(g);
        }
    }
}