package com.game.shooting_game;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.command.CommandGlassPane;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.LoginPanel;
import com.game.shooting_game.data.UserService;
import com.game.shooting_game.titleScene.TitleManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameSettings;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.LoadProgress;
import com.game.shooting_game.ui.ShooterButtonUI;
import com.game.shooting_game.ui.ShooterScrollBarUI;

public final class ShootingGameMain extends JPanel implements Runnable {
    private static final double MAX_DELTA = 0.05; // 1フレームで進める時間の上限(秒)
    private static final String component = "[MAIN]";
    private static ShootingGameMain instance;
    @Getter
    private static CommandGlassPane glassPane;

    private final CardLayout CARDLAYOUT = new CardLayout();

    @Getter
    private static Boolean onlineMode = true;

    private SceneType lastScene = null;
    @Getter
    private static double screenSizeAdj = 1;
    @Getter
    @Setter
    private int fps = GameSettings.getFps(); // FPS(端末の設定。なければ既定値)
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
        // すべてのボタンを、シューティングゲーム風の見た目にする(画面を作る前に登録する)
        ShooterButtonUI.install();
        ShooterScrollBarUI.install();

        // ターミナルのクリア
        try {
            if (System.getProperty("os.name").contains("Windows")) { // windows用
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else { // Mac,Linux用
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            LogUtils.warn(component,
                    ANSI.YELLOW + "Fail to reset logs" + ANSI.RESET);
        }

        JFrame frame = new JFrame("ShootingGame");
        ShootingGameMain panel = ShootingGameMain.getInstance();
        frame.add(panel);
        frame.pack();

        frame.setTitle("ShootingGame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.toFront(); // ウィンドウを最前面に移動
        // チュートリアルの表示(全画面に重ねる。出ていない間は隠れている)
        frame.getLayeredPane().add(com.game.shooting_game.ui.TutorialOverlay.getInstance(),
                javax.swing.JLayeredPane.POPUP_LAYER);

        panel.start();

        new Thread(() -> {
            // サーバー接続
            ConfigurableApplicationContext context = null;
            try {
                // サーバー接続開始ログ
                LogUtils.info(component,
                        ANSI.GREEN + "Connecting to game server..." + ANSI.RESET);
                LoadProgress.creep("Server connecting", 0, 0.30);

                // Spring Bootの起動
                SpringApplication app = new SpringApplication(ShootingGameApplication.class);
                // 保存を速くする既定値(application.properties に書けばそちらが優先される)。
                // まとめて送る(バッチ)ことで、DBまでの往復の回数を減らす
                app.setDefaultProperties(java.util.Map.of(
                        "spring.jpa.properties.hibernate.jdbc.batch_size", "50",
                        "spring.jpa.properties.hibernate.order_inserts", "true",
                        "spring.jpa.properties.hibernate.order_updates", "true"));
                context = app.run(args);
                DataManager.setContext(context);

                // サーバー接続成功ログ
                LogUtils.notice(component,
                        ANSI.GREEN + "Connected to game server successfully." + ANSI.RESET);
            } catch (Exception e) {
                // サーバー接続失敗ログ
                LogUtils.error(component,
                        ANSI.RED + "Server connect error" + ANSI.RESET);
                // オフライン起動ログ
                LogUtils.warn(component,
                        ANSI.YELLOW + "Starting in OFFLINE MODE." + ANSI.RESET);
                onlineMode = false;
            }
            // 初期設定開始ログ
            LogUtils.notice(component,
                    ANSI.GREEN + "Initializing Game Setup..." + ANSI.RESET);
            LoadProgress.set("Loading game configs", 0.35);

            if (onlineMode) {
                // DataManagerをSpringから取得して初期化
                context.getBean(DataManager.class);
                LoadProgress.set("Loading user data", 0.38);
            } else {
                DataManager.load("guest", "create");
            }

            LoadProgress.set("Preparing UI", 0.39);
            // GlassPaneの設定
            glassPane = new CommandGlassPane(() -> {
            });
            frame.setGlassPane((glassPane));

            // 「/」でコマンドウィンドウを起動できるようにする
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('/'), "openCmd");
            panel.getActionMap().put("openCmd", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    glassPane.showCommandBar("/");
                }
            });
            // 「t」でチャットボックスを開く
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('t'), "openChat");
            panel.getActionMap().put("openChat", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    glassPane.showCommandBar("");
                }
            });

            // リソース情報の読み込み
            LoadProgress.set("Loading resources", 0.40);
            ResourceManager.getInstance();
            LoadProgress.set("Loading scenes", 0.50);

            // UIのセットアップ
            var scenes = new ArrayList<>(SceneManager.getInstance().getSceneMap().entrySet());
            for (int i = 0; i < scenes.size(); i++) {
                LoadProgress.set("Building " + scenes.get(i).getKey().name(), 0.80 + 0.20 * i / scenes.size());
                scenes.get(i).getValue().initComponents();
            }

            // すべてのマネージャーをSceneManagerから取得しカードレイアウトに登録
            SceneManager.getInstance().getSceneMap().forEach((type, manager) -> {
                if (manager instanceof JPanel p) {
                    panel.add(p, type.name());
                }
            });

            LoadProgress.set("Ready", 1);
            panel.setLoaded(true);

            if (onlineMode) {
                UserService userService = context.getBean(UserService.class);
                LoginPanel loginPanel = new LoginPanel(TitleManager.getInstance(), userService, glassPane);
                loginPanel.showLoginPanel();
            }

            // 初期設定完了ログ
            LogUtils.notice(component,
                    ANSI.GREEN + "Game configuration loaded successfully." + ANSI.RESET);
            // 起動ログ
            LogUtils.notice(component,
                    ANSI.GREEN + "Game launching." + ANSI.RESET);

            // 最初の画面をTITLEに設定
            SceneManager.getInstance().changeScene(SceneType.TITLE);
        }).start();

        // ウィンドウを閉じたときにデータをセーブ
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 保存のあいだ画面が固まって見えないよう、先にウィンドウを隠す(保存と終了はそのあと)
                e.getWindow().setVisible(false);
                // データセーブ開始ログ
                LogUtils.info(component,
                        ANSI.GREEN + "Saving game data..." + ANSI.RESET);

                if (onlineMode) {
                    // データ保存
                    DataManager.save();
                }

                // Goodbyeログ
                LogUtils.notice(component,
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
        keepTimerResolution();
        long lastTime = System.nanoTime();
        double deltaTime;

        while (running) {
            try {
                long frameStart = System.nanoTime();

                // 秒単位に変換。一瞬止まった(GCなど)後に、動きが1回で大きく飛ばないよう上限を付ける
                deltaTime = Math.min(MAX_DELTA, (frameStart - lastTime) / 1_000_000_000.0);
                lastTime = frameStart;

                if (loaded)
                    update(deltaTime);

                repaint();

                // FPS制御(目標のフレーム間隔まで待つ)
                waitUntil(frameStart + (long) (1_000_000_000.0 / fps));
            } catch (Exception e) {
                // ゲームループエラー
                LogUtils.error(component,
                        "Game Loop Error.",
                        e);
            }
        }
    }

    /**
     * 指定の時刻まで待つ
     * Thread.sleepはWindowsで誤差が大きく、フレーム間隔が時々伸びてラグく見える。
     * 手前までは寝て、最後の1msほどは待ち続けて、ぴったりの時刻に合わせる。
     */
    private static void waitUntil(long targetNanos) {
        long remain;
        while ((remain = targetNanos - System.nanoTime()) > 0) {
            if (remain > 2_000_000) {
                try {
                    Thread.sleep((remain - 1_500_000) / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Windowsのタイマーの刻みを細かく保つ(長く眠るスレッドがあると、JVMが刻みを1msにする)
     */
    private static void keepTimerResolution() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {
                // 何もしない
            }
        }, "timer-resolution");
        t.setDaemon(true);
        t.start();
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

            // 進捗バーと状況の表示
            LoadProgress.draw(g, getWidth(), getHeight());
        } else {
            SceneManager.getInstance().draw(g);
        }
    }
}