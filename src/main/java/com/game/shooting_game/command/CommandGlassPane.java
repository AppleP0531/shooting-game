package com.game.shooting_game.command;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public final class CommandGlassPane extends JPanel {
    private static final String component = "[Command]";

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final ResourceManager RM;
    private final NormalStageManager NSM;
    private final UpgradeManager UM;
    private final ItemManager IM;
    private final ShipManager SHIP_M;
    private final BulletManager BM;
    private final StageManager STAGE_M;

    private MouseAdapter mouseHandler;
    private KeyAdapter keyHandler;

    private enum MoveOption {
        ONE_LINE_UP,
        ONE_LINE_DOWN,
        ONE_PAGE_UP,
        ONE_PAGE_DOWN
    }

    @FunctionalInterface
    interface CommandAction {
        void execute(CommandNode node, String[] args);
    }

    private boolean adminMode = false;

    /**
     * 管理者モードの設定(コインと素材の無制限化はDataManager側のフラグで判定する)
     */
    boolean isAdminMode() {
        return adminMode;
    }

    /**
     * ログ欄に1行足す(コマンドクラスは CommandGroup の info / success / error / usage 経由で呼ぶ)
     * 入力欄を閉じている間は、追加された行を一定時間だけ表示し直す
     */
    void addLog(LogLine.Kind kind, String text) {
        LogLine line = new LogLine(text, kind);
        runOnEdt(() -> {
            logHistoryData.addElement(line);
            if (!commandField.isVisible())
                showRecentLog();
        });
    }

    /** EDTで実行する(すでにEDTならそのまま) */
    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread())
            task.run();
        else
            SwingUtilities.invokeLater(task);
    }

    public void setAdminMode(boolean enabled) {
        adminMode = enabled;
        DataManager.setAdminMode(enabled);
    }

    final Map<String, CommandNode> rootCommands = new HashMap<>(); // 各コマンドクラスが登録する先

    private final JTextField commandField; // コマンド入力テキストフィールド
    private double spacing;

    // 入力履歴
    private final List<String> COMMAND_INPUT_HISTORY = new LinkedList<>(); // 入力履歴リスト
    private final DefaultListModel<String> INPUT_HISTORY_MODEL; // 入力履歴表示用モデル
    private final JList<String> INPUT_HISTORY_LIST; // 入力履歴表示用リスト
    private final int MAX_INPUT_HISTORY = 30; // 入力履歴の最大保持数
    private final int MAX_INPUT_HISTORY_DISPLAY = 10; // 入力履歴の最大表示数
    private String currentInput = ""; // 現在の入力内容
    private int inputHistoryIndex = -1; // 入力履歴参照用インデックス
    private int inputHistoryDiff = 0; // 入力履歴リストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxContext<String> inputHistoryData;

    // ログ
    private final List<LogLine> COMMAND_LOG_HISTORY = new LinkedList<>(); // コマンドログ履歴リスト
    private final DefaultListModel<LogLine> LOG_HISTORY_MODEL; // コマンドログ表示用モデル
    private final JList<LogLine> LOG_HISTORY_LIST; // コマンドログ表示用リスト
    private final int MAX_LOG_HISTORY = 30; // コマンドログの最大保持数
    private final int DEFAULT_LOG_HISTORY_DISPLAY = 10; // コマンドログのデフォルト表示数
    private final int MAX_LOG_HISTORY_DISPLAY = 20; // コマンドログの最大表示数
    private int logHistoryIndex = -1; // コマンドログ参照用インデックス
    private int logHistoryDiff = 0; // コマンドログリストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxContext<LogLine> logHistoryData;

    // 入力欄を閉じている間のログ表示
    private static final int CLOSED_LOG_MAX_LINES = 5; // 表示する最大行数
    private static final long CLOSED_LOG_VISIBLE_MS = 6000; // 追加されてから消えるまでの時間
    private static final long CLOSED_LOG_FADE_MS = 1000; // 消えるまでのうち、薄くなっていく時間
    private final Timer recentLogTimer; // 表示更新用(表示する行がなくなったら止める)

    private float chatOpacity = 0.8f; // デフォルト透明度

    // 予測変換
    private final List<String> COMMAND_SUGGEST = new LinkedList<>(); // 予測変換履歴リスト
    private final DefaultListModel<String> SUGGEST_MODEL; // 予測変換表示用モデル
    private final JList<String> SUGGEST_LIST; // 予測変換表示用リスト
    private final int MAX_SUGGEST = 30; // 予測変換の最大保持数
    private final int MAX_SUGGEST_DISPLAY = 20; // 予測変換の最大表示数
    private int suggestIndex = -1; // 予測変換参照用インデックス
    private int suggestDiff = 0; // 予測変換リストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxContext<String> suggestData;

    private boolean isAutoComplete = false;

    /**
     * 予測変換の1行分
     * 
     * @param display    表示する文字列
     * @param completion Tabで選んだときに入力欄へ入れる文字列
     * @param matchEnd   表示のうち入力済み(強調する)部分の終わり位置
     */
    private record Suggestion(String display, String completion, int matchEnd) {
    }

    private final List<Suggestion> suggestions = new ArrayList<>(); // 今表示している予測変換
    private final Map<String, Supplier<List<String>>> argCandidates = new HashMap<>(); // 引数ごとの候補

    public CommandGlassPane(Runnable onEnter) {
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        RM = ResourceManager.getInstance();
        NSM = NormalStageManager.getInstance();
        UM = UpgradeManager.getInstance();
        IM = ItemManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        BM = BulletManager.getInstance();
        STAGE_M = StageManager.getInstance();

        setLayout(null);
        setOpaque(false);

        // テキストボックスの設定
        spacing = 5 * ShootingGameMain.getScreenSizeAdj();
        Vector2 textBoxSize = new Vector2(MAIN.getWidth() - spacing * 2, 30 * ShootingGameMain.getScreenSizeAdj());
        commandField = new JTextField();
        commandField.setBounds(
                (int) spacing,
                (int) (MAIN.getHeight() - spacing - textBoxSize.getY()),
                (int) textBoxSize.getX(), (int) textBoxSize.getY());
        commandField.setBackground(new Color(0, 0, 0, 200)); // 背景色
        commandField.setForeground(Color.WHITE); // 文字色
        commandField.setCaretColor(Color.WHITE); // カーソル食
        commandField.setFont(new Font("Monospaced", Font.BOLD, (int) (16 * ShootingGameMain.getScreenSizeAdj()))); // 文字設定
        commandField.setBorder(BorderFactory.createLineBorder(Color.CYAN)); // 枠線
        commandField.setVisible(false);
        commandField.setFocusTraversalKeysEnabled(false); // Tabキーを予測変換の選択に使う

        // テキストフィールドが更新された時に予測変換も更新する
        commandField.getDocument().addDocumentListener(new DocumentListener() {
            // 文字が入力されたとき
            @Override
            public void insertUpdate(DocumentEvent e) {
                process();
            }

            // 文字が削除されたとき
            @Override
            public void removeUpdate(DocumentEvent e) {
                process();
            }

            // 文字が更新されたとき
            @Override
            public void changedUpdate(DocumentEvent e) {
                process();
            }

            private void process() {
                if (isAutoComplete) {
                    return;
                }

                registCommandSuggestions();
            }
        });
        add(commandField);

        // コマンド入力時の処理
        keyHandler = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_UP -> {
                        moveHistory(inputHistoryData, MoveOption.ONE_LINE_UP);
                        showChatBoxList(inputHistoryData);
                    }
                    case KeyEvent.VK_DOWN -> {
                        moveHistory(inputHistoryData, MoveOption.ONE_LINE_DOWN);
                        showChatBoxList(inputHistoryData);
                    }
                    case KeyEvent.VK_TAB -> {
                        // Tabで予測変換を順に選ぶ(Shift+Tabで逆順)
                        e.consume();
                        cycleSuggestion(e.isShiftDown() ? -1 : 1);
                    }
                    case KeyEvent.VK_ENTER -> executeCommand();
                    case KeyEvent.VK_ESCAPE -> {
                        hideCommandBar();
                        e.consume();
                    }
                    default -> hideChatBoxList(inputHistoryData);
                }
            }
        };

        // 入力中にマウスイベントをほかにとられないようにする
        mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // commandField の範囲外をクリックしたか判定
                if (!commandField.getBounds().contains(e.getPoint())) {
                    hideCommandBar();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getPreciseWheelRotation() < 0) {
                    moveHistory(logHistoryData, MoveOption.ONE_PAGE_UP);
                } else {
                    moveHistory(logHistoryData, MoveOption.ONE_PAGE_DOWN);
                }
                logHistoryData.setMaxDisplay(MAX_LOG_HISTORY_DISPLAY);
            }
        };

        // コマンド履歴の設定
        INPUT_HISTORY_MODEL = new DefaultListModel<>();
        INPUT_HISTORY_LIST = new JList<>(INPUT_HISTORY_MODEL);
        INPUT_HISTORY_LIST.setBackground(new Color(0, 0, 0, chatOpacity)); // 背景色
        INPUT_HISTORY_LIST.setForeground(Color.LIGHT_GRAY); // 文字色
        INPUT_HISTORY_LIST
                .setFont(new Font("Monospaced", Font.PLAIN, (int) (14 * ShootingGameMain.getScreenSizeAdj()))); // 文字設定
        INPUT_HISTORY_LIST.setBorder(null);
        INPUT_HISTORY_LIST.setVisible(false);
        INPUT_HISTORY_LIST.setFocusable(false); // フォーカスを当てない
        // 選択時にテキストフィールドへ反映する
        INPUT_HISTORY_LIST.addListSelectionListener(e -> {
            isAutoComplete = true;
            commandField.setText(INPUT_HISTORY_LIST.getSelectedValue());
            isAutoComplete = false;

            // フォーカスを返す
            commandField.requestFocusInWindow();
        });
        // 選択されているものを太字にする
        INPUT_HISTORY_LIST.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(list.getBackground());
                // 選択されているものを太字、そうでなければ標準で表示
                if (isSelected) {
                    setFont(list.getFont().deriveFont(Font.BOLD));
                    setForeground(Color.WHITE);
                } else {
                    setFont(list.getFont().deriveFont(Font.PLAIN));
                    setForeground(Color.LIGHT_GRAY);
                }

                return this;
            }
        });
        add(INPUT_HISTORY_LIST);
        inputHistoryData = new ChatBoxContext<>(
                COMMAND_INPUT_HISTORY,
                INPUT_HISTORY_MODEL,
                INPUT_HISTORY_LIST,
                MAX_INPUT_HISTORY,
                MAX_INPUT_HISTORY_DISPLAY,
                inputHistoryIndex,
                inputHistoryDiff);

        // ログの設定
        LOG_HISTORY_MODEL = new DefaultListModel<>();
        LOG_HISTORY_LIST = new JList<>(LOG_HISTORY_MODEL) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                // 透明度の適応
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, chatOpacity));

                super.paintComponent(g2);
                g2.dispose();
            }
        };
        LOG_HISTORY_LIST.setBackground(new Color(0, 0, 0, chatOpacity)); // 背景色
        LOG_HISTORY_LIST.setForeground(Color.LIGHT_GRAY); // 文字色
        LOG_HISTORY_LIST.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * ShootingGameMain.getScreenSizeAdj()))); // 文字設定
        LOG_HISTORY_LIST.setBorder(null);
        LOG_HISTORY_LIST.setVisible(false);
        LOG_HISTORY_LIST.setFocusable(false); // フォーカスを当てない
        // 種別ごとの色・記号で描画
        LOG_HISTORY_LIST.setCellRenderer(new LogCellRenderer());
        add(LOG_HISTORY_LIST);

        // 入力欄を閉じている間のログ表示の更新(薄くなる動きと消去)
        recentLogTimer = new Timer(50, e -> showRecentLog());

        logHistoryData = new ChatBoxContext<>(
                COMMAND_LOG_HISTORY,
                LOG_HISTORY_MODEL,
                LOG_HISTORY_LIST,
                MAX_LOG_HISTORY,
                DEFAULT_LOG_HISTORY_DISPLAY,
                logHistoryIndex,
                logHistoryDiff);

        // 予測変換の設定
        SUGGEST_MODEL = new DefaultListModel<>();
        SUGGEST_LIST = new JList<>(SUGGEST_MODEL);
        SUGGEST_LIST.setBackground(new Color(0, 0, 0, chatOpacity)); // 背景色
        SUGGEST_LIST.setForeground(Color.LIGHT_GRAY); // 文字色
        SUGGEST_LIST.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * ShootingGameMain.getScreenSizeAdj()))); // 文字設定
        SUGGEST_LIST.setBorder(null);
        SUGGEST_LIST.setVisible(false);
        // 入力に応じて色を付ける
        SUGGEST_LIST.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(list.getBackground());

                if (value != null) {
                    String text = value.toString();
                    int matchEnd = (index >= 0 && index < suggestions.size()) ? suggestions.get(index).matchEnd() : 0;
                    matchEnd = Math.max(0, Math.min(text.length(), matchEnd));

                    // エスケープ文字に変換
                    String matchText = text.substring(0, matchEnd).replace("<", "&lt;").replace(">", "&gt;");
                    String remainText = text.substring(matchEnd).replace("<", "&lt;").replace(">", "&gt;");

                    String htmlText = "<html><font face='Monospaced'>" +
                            "<b><font color='white'>" + matchText + "</font></b>" +
                            "<font color='#808080'>" + remainText + "</font>" +
                            "</font></html>";
                    setText(htmlText);

                    // Tabで選んでいる行は背景を明るくする
                    if (isSelected)
                        setBackground(new Color(70, 70, 70));
                }

                return this;
            }
        });
        add(SUGGEST_LIST);
        suggestData = new ChatBoxContext<>(
                COMMAND_SUGGEST,
                SUGGEST_MODEL,
                SUGGEST_LIST,
                MAX_SUGGEST,
                MAX_SUGGEST_DISPLAY,
                suggestIndex,
                suggestDiff);

        registArgCandidates();
        registCommands();
    }

    /**
     * 履歴の移動
     * 
     * @param context チャットボックス情報
     * @param diff    移動量
     * @return 次の参照インデックス
     */
    private void moveHistory(ChatBoxContext<?> context, MoveOption diff) {
        if (context.getElements().isEmpty())
            return;

        int displayListSize = Math.min(context.getMaxDisplay(), context.getElements().size());

        // 履歴移動処理
        switch (diff) {
            case MoveOption.ONE_LINE_UP -> {
                if (context.getIndex() == -1) {
                    // 現在の入力を保存
                    currentInput = commandField.getText();
                    context.setIndex(displayListSize - 1);
                } else {
                    // 表示履歴の一番上に到達したら1行上にスクロールする
                    if (context.getIndex() == 0 && context.getElements().size() >= context.getMaxDisplay())
                        context.setDiff(Math.min(context.getDiff() + 1,
                                context.getElements().size() - context.getMaxDisplay()));

                    context.setIndex(Math.max(0, context.getIndex() - 1));
                }
            }
            case MoveOption.ONE_LINE_DOWN -> {
                if (context.getIndex() == -1)
                    return;

                if (context.getDiff() != 0 || context.getIndex() < displayListSize - 1) {
                    // 表示履歴の一番下に到達したら一行下にスクロールする
                    if (context.getIndex() >= context.getMaxDisplay() - 1)
                        context.setDiff(Math.max(0, context.getDiff() - 1));

                    context.setIndex(Math.min(context.getMaxDisplay() - 1, context.getIndex() + 1));
                } else
                    context.setIndex(-1);
            }
            case MoveOption.ONE_PAGE_UP -> {
                if (context.getElements().size() <= context.getMaxDisplay())
                    context.setDiff(0);
                else
                    context.setDiff(
                            Math.min(context.getDiff() + 1, context.getElements().size() - context.getMaxDisplay()));
            }
            case MoveOption.ONE_PAGE_DOWN -> {
                context.setDiff(Math.max(0, context.getDiff() - 1));
            }
        }

        // UIへの反映
        showChatBoxList(context);
        context.getList().setSelectedIndex(context.getIndex());

        // コマンド履歴をさかのぼっているときに限り、テキストボックスを上書き
        if (context == inputHistoryData)
            commandField.setText(context.getIndex() == -1 ? currentInput
                    : (String) context.getElements().get(context.getElements().size() - 1
                            - (displayListSize - 1 - context.getIndex()) - context.getDiff()));
    }

    /**
     * コマンドウィンドウの表示
     * 
     * @param text デフォルト挿入文字
     */
    public void showCommandBar(String text) {
        // ログイン前(ユーザーデータがない間)は、コマンドもチャットも使えない
        if (DataManager.getUserData() == null)
            return;

        addHandler();

        recentLogTimer.stop(); // 閉じている間の簡易表示は終わり
        setVisible(true);
        commandField.setVisible(true);
        commandField.requestFocusInWindow();

        showChatBoxList(logHistoryData);
        commandField.setText(text);
    }

    /**
     * コマンドウィンドウの非表示
     */
    public void hideCommandBar() {
        removeHandler();
        commandField.setVisible(false);

        hideAllChatBox();

        LOG_HISTORY_LIST.setOpaque(false);

        logHistoryData.setIndex(-1);
        logHistoryData.setMaxDisplay(DEFAULT_LOG_HISTORY_DISPLAY);
        suggestData.setIndex(-1);

        // 閉じたあとも、最近のログだけ一定時間表示する
        showRecentLog();
    }

    /**
     * グラスペインがマウスを受け取るのは入力欄を開いている間だけ
     * (閉じている間のログ表示はゲームの操作を邪魔しない)
     */
    @Override
    public boolean contains(int x, int y) {
        return commandField.isVisible() && super.contains(x, y);
    }

    /**
     * 入力欄を閉じている間のログ表示(最近の数行を、時間とともに薄くして消す)
     * 表示する行がなければタイマーを止めてグラスペインを隠す
     */
    private void showRecentLog() {
        if (commandField.isVisible()) {
            recentLogTimer.stop();
            return;
        }

        long now = System.currentTimeMillis();
        List<LogLine> recent = new ArrayList<>();
        List<LogLine> all = logHistoryData.getElements();
        for (int i = all.size() - 1; i >= 0 && recent.size() < CLOSED_LOG_MAX_LINES; i--) {
            LogLine line = all.get(i);
            if (now - line.createdAt() >= CLOSED_LOG_VISIBLE_MS)
                break; // 古い行より前はさらに古い
            recent.addFirst(line);
        }

        if (recent.isEmpty()) {
            recentLogTimer.stop();
            LOG_HISTORY_LIST.setVisible(false);
            LOG_HISTORY_MODEL.clear();
            setVisible(false);
            return;
        }

        // 行が変わったときだけモデルと配置を作り直す(薄くなる動きは再描画のみ)
        boolean changed = recent.size() != LOG_HISTORY_MODEL.size();
        for (int i = 0; !changed && i < recent.size(); i++) {
            changed = !recent.get(i).equals(LOG_HISTORY_MODEL.get(i));
        }
        if (changed || !LOG_HISTORY_LIST.isVisible()) {
            LOG_HISTORY_MODEL.clear();
            LOG_HISTORY_MODEL.addAll(recent);
            placeLogList();
            LOG_HISTORY_LIST.setVisible(true);
        }

        setVisible(true);
        LOG_HISTORY_LIST.repaint();
        if (!recentLogTimer.isRunning())
            recentLogTimer.start();
    }

    /**
     * ログ1行あたりの高さ
     */
    private int logRowHeight() {
        return (int) Math.round(18 * ShootingGameMain.getScreenSizeAdj());
    }

    /**
     * ログリストを入力欄の真上に配置する(開閉どちらでも同じ位置)
     */
    private void placeLogList() {
        int rowHeight = logRowHeight();
        LOG_HISTORY_LIST.setFixedCellHeight(rowHeight);
        int listHeight = LOG_HISTORY_MODEL.size() * rowHeight;
        LOG_HISTORY_LIST.setBounds(
                commandField.getX(),
                commandField.getY() - listHeight - 1,
                commandField.getWidth(),
                listHeight);
    }

    /**
     * 履歴リストの表示
     *
     * @param context チャットボックス情報
     */
    private <T> void showChatBoxList(ChatBoxContext<T> context) {
        int savedIndex = context.getIndex();
        hideAllChatBox();

        // データがなければ表示しない
        if (context.getElements().size() == 0)
            return;

        context.getModel().clear();

        int start = Math.max(0, context.getElements().size() - context.getDiff() - context.getMaxDisplay());
        for (int i = start; i < context.getElements().size() - context.getDiff(); i++) {
            context.getModel().addElement(context.getElements().get(i));
        }

        // 配置の計算と設定(入力欄の真上)
        repaint();
        if (context == logHistoryData) {
            placeLogList();
        } else {
            int rowHeight = 20; // 1行あたりの高さ
            context.getList().setFixedCellHeight(rowHeight);
            int listHeight = context.getModel().size() * rowHeight;
            context.getList().setBounds(
                    commandField.getX(),
                    commandField.getY() - listHeight - 1,
                    commandField.getWidth(),
                    listHeight);
        }

        context.getList().setSelectedIndex(savedIndex);
        context.getList().setVisible(true);
    }

    /**
     * 履歴リストの非表示
     * 
     * @param context チャットボックス情報
     */
    private void hideChatBoxList(ChatBoxContext<?> context) {
        context.getList().setVisible(false);
    }

    /**
     * すべてのチャットボックスを隠す
     */
    private void hideAllChatBox() {
        hideChatBoxList(inputHistoryData);
        hideChatBoxList(logHistoryData);
        hideChatBoxList(suggestData);
    }

    /**
     * 入力処理
     */
    private void executeCommand() {
        // 入力内容があれば処理、履歴に追加
        if (!commandField.getText().isEmpty()) {
            processCommand(commandField.getText());

            // ひとつ前のコマンドと内容が違ければ履歴に追加
            if (inputHistoryData.getElements().isEmpty()
                    || !inputHistoryData.getElements().getLast().equals(commandField.getText()))

                inputHistoryData.addElement(commandField.getText());
        }

        // リセット
        inputHistoryData.setIndex(-1);
        currentInput = "";
        hideCommandBar();
    }

    /**
     * コマンド処理
     * 
     * @param text 入力内容
     */
    private void processCommand(String text) {
        // "/"から始まっていなかったらログにそのまま出力して終了
        if (!text.startsWith("/")) {
            addLog(LogLine.Kind.CHAT, chatUserName() + " " + text);
            LogUtils.debugLog(text);
            return;
        }

        String[] args = text.split(" ", -1);
        CommandNode node = findCommand(rootCommands, args[0]);

        // コマンドリストになかった場合
        if (node == null) {
            unknownCommand(text);
            return;
        }

        // 階層探索
        while (args.length > 0) {
            // 権限チェック
            if (node.isAdminOnly() && !adminMode) {
                unknownCommand(text);
                break;
            }

            // 与えられえたサブ引数が存在する場合は一つ階層を潜る、なければ処理
            args = Arrays.copyOfRange(args, 1, args.length);
            CommandNode subNode = args.length > 0 ? findCommand(node.getSubCommands(), args[0]) : null;
            if (subNode != null) {
                node = subNode;
            } else if (node.getAction() != null) {
                node.getAction().execute(node, args);
                break;
            } else {
                unknownCommand(text);
                break;
            }
        }
    }

    /**
     * チャットに表示する自分の名前(ログイン前などで取れなければ guest)
     */
    private String chatUserName() {
        var user = DataManager.getUserData();
        String name = user == null ? null : user.getUsername();
        return (name == null || name.isBlank()) ? "guest" : name.replaceAll("\\s+", "_");
    }

    /**
     * 大文字小文字を区別せずにコマンド(サブコマンド)を探す
     * 
     * @param nodes 探す対象
     * @param token 入力されたコマンド名
     * @return 見つかったノード、なければnull
     */
    private CommandNode findCommand(Map<String, CommandNode> nodes, String token) {
        CommandNode exact = nodes.get(token);
        if (exact != null)
            return exact;

        for (Map.Entry<String, CommandNode> entry : nodes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(token))
                return entry.getValue();
        }
        return null;
    }

    /**
     * 引数の候補の登録
     * キーはsyntaxの<>や[]を除いた名前
     */
    private void registArgCandidates() {
        // 敵・ボス(データのあるもの)
        argCandidates.put("enemyId", () -> {
            List<String> ids = new ArrayList<>();
            for (ShipKinds kind : new ShipKinds[] { ShipKinds.ENEMY, ShipKinds.BOSS }) {
                SHIP_M.getShipConfig().get(kind).forEach((id, def) -> {
                    if (def != null && def.getDatas() != null)
                        ids.add(id);
                });
            }
            ids.sort(String.CASE_INSENSITIVE_ORDER);
            return ids;
        });
        argCandidates.put("mode", () -> List.of("blue", "red"));
        argCandidates.put("target", () -> List.of("player", "enemy", "boss", "all"));
        argCandidates.put("shipId", () -> SHIP_M.getShipConfig().get(ShipKinds.PLAYER).keySet().stream()
                .map(String::toLowerCase).sorted().toList());
        argCandidates.put("sceneName", () -> Arrays.stream(SceneType.values())
                .map(type -> type.name().toLowerCase()).toList());
        argCandidates.put("resourceName", () -> Arrays.stream(ResourceName.values())
                .map(name -> name.name().toLowerCase()).toList());
    }

    /**
     * 予測変換の追加
     */
    private void addSuggestion(String display, String completion, int matchEnd) {
        if (suggestions.size() >= MAX_SUGGEST_DISPLAY)
            return;
        for (Suggestion s : suggestions) {
            if (s.display().equals(display))
                return;
        }
        suggestions.add(new Suggestion(display, completion, matchEnd));
        suggestData.addElement(display);
    }

    /**
     * Tabによる予測変換の選択(選んだものを入力欄に反映する)
     * 
     * @param dir 1で次、-1で前
     */
    private void cycleSuggestion(int dir) {
        if (suggestions.isEmpty())
            return;

        int size = suggestions.size();
        int index = suggestData.getIndex();
        index = index < 0 ? (dir > 0 ? 0 : size - 1) : Math.floorMod(index + dir, size);
        suggestData.setIndex(index);

        // 入力欄を更新しても予測変換は作り直さない
        isAutoComplete = true;
        commandField.setText(suggestions.get(index).completion());
        isAutoComplete = false;

        showChatBoxList(suggestData);
    }

    /**
     * 予測変換リストの作成
     */
    private void registCommandSuggestions() {
        hideChatBoxList(suggestData);

        // リストのクリア
        suggestData.getElements().clear();
        suggestions.clear();
        suggestData.setIndex(-1);

        // 入力情報の取得
        String command = commandField.getText();
        if (command.isEmpty()) {
            showChatBoxList(logHistoryData);
            return;
        }
        hideChatBoxList(logHistoryData);

        String[] tokens = command.split(" ", -1);
        String partial = tokens[tokens.length - 1]; // 入力中の単語

        // 入力済みのコマンド名をたどる(大文字小文字は区別しない)
        Map<String, CommandNode> level = rootCommands;
        CommandNode current = null;
        int consumed = 0; // コマンド名として使われた単語の数
        for (int i = 0; i < tokens.length - 1; i++) {
            CommandNode next = findCommand(level, tokens[i]);
            if (next == null)
                break;
            // 権限不足の場合は出さない
            if (next.isAdminOnly() && !adminMode)
                return;

            current = next;
            level = next.getSubCommands();
            consumed = i + 1;
        }
        // 最初の単語がコマンドではない
        if (tokens.length > 1 && current == null)
            return;

        int argPos = tokens.length - 1 - consumed; // 入力中の引数の番号(0始まり)
        String prefix = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 1));
        String prefixSp = prefix.isEmpty() ? "" : prefix + " ";
        String lowerPartial = partial.toLowerCase();

        // コマンド名の候補(コマンドの直後の単語を入力中のときだけ)
        boolean hasSubMatch = false;
        if (argPos == 0) {
            for (CommandNode node : level.values()) {
                if (!node.getName().toLowerCase().startsWith(lowerPartial))
                    continue;

                for (String usage : node.getAllNodes(adminMode)) {
                    hasSubMatch = true;
                    // コマンド名の部分(最初の<や[の手前まで)を補完内容にする
                    String[] words = usage.split(" ");
                    List<String> path = new ArrayList<>();
                    for (String word : words) {
                        if (word.startsWith("<") || word.startsWith("["))
                            break;
                        path.add(word);
                    }
                    String completion = String.join(" ", path) + (path.size() < words.length ? " " : "");
                    int matchEnd = usage.toLowerCase().startsWith(prefixSp.toLowerCase() + lowerPartial)
                            ? (prefixSp + partial).length()
                            : 0;
                    addSuggestion(usage, completion, matchEnd);
                }
            }
        }

        // 引数の候補(入力中の引数に対応するsyntax)
        if (current != null && current.getSyntax() != null) {
            List<String> syntax = current.getSyntax().stream().filter(s -> !s.isBlank()).toList();
            if (argPos < syntax.size()) {
                String placeholder = syntax.get(argPos);
                String key = placeholder.replaceAll("[<>\\[\\]]", "");
                String rest = String.join(" ", syntax.subList(argPos + 1, syntax.size()));
                Supplier<List<String>> provider = argCandidates.get(key);

                if (provider != null) {
                    // 入力に合う候補を出す
                    for (String candidate : provider.get()) {
                        if (!candidate.toLowerCase().startsWith(lowerPartial))
                            continue;
                        // サブコマンドと同名のものは重複するので出さない
                        if (argPos == 0 && findCommand(level, candidate) != null)
                            continue;

                        String head = prefixSp + candidate;
                        addSuggestion(rest.isEmpty() ? head : head + " " + rest,
                                rest.isEmpty() ? head : head + " ",
                                (prefixSp + partial).length()); // 入力済みの文字までを強調
                    }
                } else if (!(argPos == 0 && hasSubMatch && !partial.isEmpty())) {
                    // 自由入力: 入力済みの値を入れた使い方を表示する
                    String shown = partial.isEmpty() ? placeholder : partial;
                    String head = prefixSp + shown;
                    addSuggestion(rest.isEmpty() ? head : head + " " + rest, command,
                            (prefixSp + partial).length()); // 入力済みの文字までを強調(未入力なら強調なし)
                }
            }
        }

        if (!suggestions.isEmpty())
            showChatBoxList(suggestData);
    }

    /**
     * コマンドの登録(種類ごとのクラスに分けてある)
     */
    private void registCommands() {
        new SystemCommands(this).registerAll();
        new GameCommands(this).registerAll();
        new PlayerDataCommands(this).registerAll();
    }

    /**
     * ヘルプコマンドによる出力
     */
    void showHelp() {
        LogUtils.debugLog("");

        addLog(LogLine.Kind.HELP, "----------------- Available Commands -----------------");
        rootCommands.entrySet().stream()
                .filter(e -> !e.getValue().isAdminOnly() || adminMode) // 権限があるもの
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> addLog(LogLine.Kind.HELP, e.getKey() + " : " + e.getValue().getDescription()));
        LogUtils.debugLog(ANSI.YELLOW + "----------------- Available Commands -----------------" + ANSI.RESET);
        rootCommands.entrySet().stream()
                .filter(e -> !e.getValue().isAdminOnly() || adminMode) // 権限があるもの
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> LogUtils.debugLog("%-15s : %s%n", e.getKey(), e.getValue().getDescription()));

        LogUtils.debugLog("");
    }

    /**
     * リソースの再読み込み
     */
    void reload() {
        // リロード開始ログ
        String logText = "Attempting to reload resources...";
        addLog(LogLine.Kind.INFO, logText);
        LogUtils.info(component, logText);

        RM.reload();
        IM.reload();
        SHIP_M.reload();
        STAGE_M.reload();
        BM.reload();
        NSM.reload();
        UM.reload();
        SM.getCurrentManager().setup();

        // テストシーンの場合画面ごとリログ
        if (SM.getSceneName() == SceneType.TEST) {
            SM.changeScene(SceneType.TEST);
        }

        // リロード完了ログ
        logText = "Reload complete.";
        addLog(LogLine.Kind.SUCCESS, logText);
        LogUtils.info(component, logText);
    }

    /**
     * 未知コマンドに対するログ
     * 
     * @param text 未知コマンド内容
     */
    void unknownCommand(String text) {
        String logText = "Unknown command '" + text + "'";
        addLog(LogLine.Kind.ERROR, logText);
        LogUtils.debugLog(logText);
        logText = "Type '/help' for help.";
        addLog(LogLine.Kind.HELP, logText);
        LogUtils.debugLog(logText);
    }

    /**
     * ログ1行の描画(種別ごとの色と行頭の記号、はみ出す行は末尾を … で省略)
     * 入力欄を閉じている間は、消える直前の1秒で行ごとに薄くする
     */
    private final class LogCellRenderer extends JComponent implements ListCellRenderer<LogLine> {
        private static final Color COLOR_INFO = new Color(220, 220, 220);
        private static final Color COLOR_SUCCESS = new Color(0x5C, 0xE0, 0x8A);
        private static final Color COLOR_ERROR = new Color(0xFF, 0x6B, 0x6B);
        private static final Color COLOR_HELP = new Color(0x7F, 0xD8, 0xFF);
        private static final Color COLOR_CHAT = Color.WHITE;
        private static final Color COLOR_CHAT_NAME = new Color(0xFF, 0xDF, 0x80); // チャットの名前部分

        private LogLine line;
        private Color background = Color.BLACK;

        @Override
        public Component getListCellRendererComponent(JList<? extends LogLine> list, LogLine value, int index,
                boolean isSelected, boolean cellHasFocus) {
            line = value;
            background = list.getBackground();
            setFont(list.getFont().deriveFont(Font.PLAIN));
            return this;
        }

        /** 行頭の記号(フォントが対応していなければ ASCII にする) */
        private String prefixOf(LogLine.Kind kind, Font font) {
            boolean unicode = font.canDisplayUpTo("✓✖") == -1;
            return switch (kind) {
                case SUCCESS -> unicode ? "✓ " : "+ ";
                case ERROR -> unicode ? "✖ " : "! ";
                case HELP -> "? ";
                default -> "";
            };
        }

        private Color colorOf(LogLine.Kind kind) {
            return switch (kind) {
                case SUCCESS -> COLOR_SUCCESS;
                case ERROR -> COLOR_ERROR;
                case HELP -> COLOR_HELP;
                case CHAT -> COLOR_CHAT;
                default -> COLOR_INFO;
            };
        }

        /** 閉じている間の透明度(消える直前の1秒で 1 から 0 へ) */
        private float fadeOf(LogLine target) {
            if (commandField.isVisible())
                return 1f;
            long remain = CLOSED_LOG_VISIBLE_MS - (System.currentTimeMillis() - target.createdAt());
            return Math.max(0f, Math.min(1f, remain / (float) CLOSED_LOG_FADE_MS));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (line == null)
                return;
            Graphics2D g2 = (Graphics2D) g.create();

            // リスト側の透明度に、行ごとの薄くなる分を掛ける
            float base = g2.getComposite() instanceof AlphaComposite ac ? ac.getAlpha() : 1f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, base * fadeOf(line)));

            g2.setColor(background);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            String prefix = prefixOf(line.kind(), font);
            String full = prefix + line.text();

            // はみ出す場合は末尾を省略
            int pad = (int) (4 * ShootingGameMain.getScreenSizeAdj());
            int avail = getWidth() - pad * 2;
            if (fm.stringWidth(full) > avail) {
                int end = full.length();
                while (end > 0 && fm.stringWidth(full.substring(0, end) + "…") > avail)
                    end--;
                full = full.substring(0, end) + "…";
            }

            // 色の境目: [記号][チャットの名前][本文]
            int prefixEnd = Math.min(prefix.length(), full.length());
            int nameEnd = prefixEnd;
            if (line.kind() == LogLine.Kind.CHAT) {
                int space = line.text().indexOf(' ');
                nameEnd = Math.min(full.length(), prefix.length() + Math.max(0, space));
            }

            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int x = pad;
            Color main = colorOf(line.kind());
            String[] parts = { full.substring(0, prefixEnd), full.substring(prefixEnd, nameEnd),
                    full.substring(nameEnd) };
            Color[] colors = { main, COLOR_CHAT_NAME, main };
            for (int i = 0; i < parts.length; i++) {
                g2.setColor(colors[i]);
                g2.drawString(parts[i], x, y);
                x += fm.stringWidth(parts[i]);
            }
            g2.dispose();
        }
    }

    /**
     * マウスハンドラの追加
     */
    private void addHandler() {
        addMouseListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        commandField.addKeyListener(keyHandler);
    }

    /**
     * マウスハンドラの削除
     */
    private void removeHandler() {
        removeMouseListener(mouseHandler);
        removeMouseWheelListener(mouseHandler);
        commandField.removeKeyListener(keyHandler);
    }
}