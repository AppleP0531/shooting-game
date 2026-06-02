package com.game.shooting_game.command;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lombok.Setter;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.DataManager.UserData;
import com.game.shooting_game.data.DataManager.UserData.Resources;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.lobbyScene.LobbyManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public final class CommandGlassPane extends JPanel {
    private static final String component = "[Command]";

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final ResourceManager RM;
    private final NormalStageManager NSM;
    private final UpgradeManager UM;
    private final GameManager GM;
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

    @Setter
    private boolean adminMode = false;

    private Map<String, CommandNode> rootCommands = new HashMap<>();

    private final JTextField commandField; // コマンド入力テキストフィールド
    private String logText;

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
    private ChatBoxContext inputHistoryData;

    // ログ
    private final List<String> COMMAND_LOG_HISTORY = new LinkedList<>(); // コマンドログ履歴リスト
    private final DefaultListModel<String> LOG_HISTORY_MODEL; // コマンドログ表示用モデル
    private final JList<String> LOG_HISTORY_LIST; // コマンドログ表示用リスト
    private final int MAX_LOG_HISTORY = 30; // コマンドログの最大保持数
    private final int DEFAULT_LOG_HISTORY_DISPLAY = 10; // コマンドログのデフォルト表示数
    private final int MAX_LOG_HISTORY_DISPLAY = 20; // コマンドログの最大表示数
    private int logHistoryIndex = -1; // コマンドログ参照用インデックス
    private int logHistoryDiff = 0; // コマンドログリストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxContext logHistoryData;

    private Timer fadeTimer; // フェードアウト用タイマー
    private float defaultOpacity = 0.8f; // デフォルト透明度
    private float chatOpacity = defaultOpacity; // 透明度
    private int delay = 50; // フェードアウト更新速度(ミリ秒)
    private int delayCounter = 0;
    private final int initialDelay = 5000 / delay; // 待機時間(ミリ秒/delay)

    // 予測変換
    private final List<String> COMMAND_SUGGEST = new LinkedList<>(); // 予測変換履歴リスト
    private final DefaultListModel<String> SUGGEST_MODEL; // 予測変換表示用モデル
    private final JList<String> SUGGEST_LIST; // 予測変換表示用リスト
    private final int MAX_SUGGEST = 30; // 予測変換の最大保持数
    private final int MAX_SUGGEST_DISPLAY = 20; // 予測変換の最大表示数
    private int suggestIndex = -1; // 予測変換参照用インデックス
    private int suggestDiff = 0; // 予測変換リストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxContext suggestData;

    private boolean isAutoComplete = false;

    public CommandGlassPane(Runnable onEnter) {
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        RM = ResourceManager.getInstance();
        NSM = NormalStageManager.getInstance();
        UM = UpgradeManager.getInstance();
        GM = GameManager.getInstance();
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
        inputHistoryData = new ChatBoxContext(
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
        LOG_HISTORY_LIST.setFont(new Font("Monospaced", Font.PLAIN, (int) (10 * ShootingGameMain.getScreenSizeAdj()))); // 文字設定
        LOG_HISTORY_LIST.setBorder(null);
        LOG_HISTORY_LIST.setVisible(false);
        LOG_HISTORY_LIST.setFocusable(false); // フォーカスを当てない
        // 文字装飾なしで描画
        LOG_HISTORY_LIST.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(list.getBackground());
                setFont(list.getFont().deriveFont(Font.PLAIN));
                setForeground(Color.LIGHT_GRAY);

                return this;
            }
        });
        add(LOG_HISTORY_LIST);
        logHistoryData = new ChatBoxContext(
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
                    String fullText = value.toString();
                    String inputText = commandField.getText();

                    String[] args = inputText.split(" ");
                    String[] tokens = fullText.split(" ");

                    String matchText = "";
                    String remainText = "";

                    for (int i = 0; i < args.length - 1; i++) {
                        matchText += tokens[i] + " ";
                    }

                    String comparingToken = tokens[args.length - 1];
                    Boolean isSyntax = comparingToken.startsWith("<");
                    if (isSyntax) {
                        matchText += comparingToken + " ";
                        for (int i = args.length; i < tokens.length; i++) {
                            remainText += tokens[i] + " ";
                        }
                    } else {
                        matchText += fullText.substring(matchText.length(), inputText.length());
                        remainText = fullText.substring(inputText.length());
                    }

                    // エスケープ文字に変換
                    matchText = matchText.replace("<", "&lt;").replace(">", "&gt;");
                    remainText = remainText.replace("<", "&lt;").replace(">", "&gt;");

                    String htmlText = "<html><font face='Monospaced'>" +
                            "<b><font color='white'>" + matchText + "</font></b>" +
                            "<font color='#808080'>" + remainText + "</font>" +
                            "</font></html>";
                    setText(htmlText);
                }

                return this;
            }
        });
        add(SUGGEST_LIST);
        suggestData = new ChatBoxContext(
                COMMAND_SUGGEST,
                SUGGEST_MODEL,
                SUGGEST_LIST,
                MAX_SUGGEST,
                MAX_SUGGEST_DISPLAY,
                suggestIndex,
                suggestDiff);

        initFadeTimer();
        registCommands();
    }

    /**
     * 履歴の移動
     * 
     * @param context チャットボックス情報
     * @param diff    移動量
     * @return 次の参照インデックス
     */
    private void moveHistory(ChatBoxContext context, MoveOption diff) {
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
                    : context.getElements().get(context.getElements().size() - 1
                            - (displayListSize - 1 - context.getIndex()) - context.getDiff()));
    }

    /**
     * コマンドウィンドウの表示
     * 
     * @param text デフォルト挿入文字
     */
    public void showCommandBar(String text) {
        addHandler();
        chatOpacity = defaultOpacity;

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
        showChatBoxList(logHistoryData);

        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
        delayCounter = 0;
        chatOpacity = defaultOpacity;

        LOG_HISTORY_LIST.setOpaque(false);

        fadeTimer.start();

        logHistoryData.setIndex(-1);
        logHistoryData.setMaxDisplay(DEFAULT_LOG_HISTORY_DISPLAY);
        suggestData.setIndex(-1);
    }

    /**
     * 履歴リストの表示
     * 
     * @param context チャットボックス情報
     */
    private void showChatBoxList(ChatBoxContext context) {
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

        // リスト全体の高さを計算
        int rowHeight = 20; // 1行あたりの高さ
        context.getList().setFixedCellHeight(rowHeight);

        // 配置の計算と設定
        repaint();
        int listHeight = (context.getModel().size() * rowHeight);
        if (commandField.isVisible()) {
            context.getList().setBounds(
                    commandField.getX(),
                    commandField.getY() - listHeight - 1,
                    commandField.getWidth(),
                    listHeight);
        } else {
            context.getList().setBounds(
                    (int) spacing,
                    (int) (MAIN.getHeight() - spacing - listHeight - 1),
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
    private void hideChatBoxList(ChatBoxContext context) {
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

    private void initFadeTimer() {
        // 一定時間待機した後、50ミリ秒ごとに不透明度減らす
        fadeTimer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 入力バーが表示されている間はデフォルト値
                if (commandField.isVisible()) {
                    chatOpacity = defaultOpacity;
                }

                // 一定時間待機
                if (delayCounter < initialDelay) {
                    delayCounter++;
                    return;
                }

                // 0.05ずつ透明にする
                chatOpacity -= 0.05f;
                if (chatOpacity <= 0.0f) {
                    fadeTimer.stop(); // 透明になったらタイマーを止める
                    chatOpacity = 0.0f;
                    setVisible(false);
                    hideChatBoxList(logHistoryData); // 非表示にする
                }

                LOG_HISTORY_LIST.repaint();
            }
        });
    }

    /**
     * 入力処理
     */
    private void executeCommand() {
        // 入力内容があれば処理、履歴に追加
        if (!commandField.getText().isEmpty()) {
            processCommand(commandField.getText());

            // 履歴に追加
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
            logHistoryData.addElement(text);
            LogUtils.debugLog(text);
            return;
        }

        String[] args = text.split(" ");
        CommandNode node = rootCommands.get(args[0]);

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
            if (args.length > 0 && node.getSubCommands().containsKey(args[0])) {
                node = node.getSubCommands().get(args[0]);
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
     * 第一コマンド登録
     * 
     * @param root   親要素
     * @param name   コマンド名
     * @param desc   説明
     * @param admin  管理者権限が必要かどうか
     * @param action 処理
     * @return コマンドノード
     */
    private CommandNode register(Map<String, CommandNode> root, String name, String desc, boolean admin,
            CommandAction action) {
        CommandNode node = new CommandNode(null, name, desc, admin, action);
        root.put(name, node);
        return node;
    }

    /**
     * サブコマンド登録
     * 
     * @param root   親要素
     * @param name   コマンド名
     * @param desc   説明
     * @param admin  管理者権限が必要かどうか
     * @param action 処理
     * @return コマンドノード
     */
    private CommandNode register(CommandNode root, String name, String desc, boolean admin,
            CommandAction action) {
        CommandNode node = new CommandNode(root, name, desc, admin, action);
        root.getSubCommands().put(name, node);
        return node;
    }

    /**
     * 予測変換リストの作成
     */
    private void registCommandSuggestions() {
        hideChatBoxList(suggestData);

        // リストのクリア
        suggestData.getElements().clear();

        // 入力情報の取得
        String command = commandField.getText();
        String[] args = command.split(" ", -1);

        if (command.isEmpty()) {
            showChatBoxList(logHistoryData);
            return;
        }

        hideChatBoxList(logHistoryData);

        Map<String, CommandNode> subCommands = rootCommands; // 再外層ノードから探索
        CommandNode currentNode = null; // 参照中ノード
        // 引数と一致する子ノードを見つけて階層を移動
        for (int i = 0; i < args.length - 1; i++) {
            CommandNode node = subCommands.get(args[i]);

            if (node != null) {
                // 権限不足の場合は潜らない
                if (node.isAdminOnly() && !adminMode)
                    return;

                subCommands = node.getSubCommands(); // 階層移動
                currentNode = node; // 参照中ノードの更新
            } else
                return;
        }
        String currentToken = args[args.length - 1];

        // 入力中文字と比較
        for (CommandNode node : subCommands.values()) {
            if (node.getName().startsWith(currentToken)) {
                for (String suggests : node.getAllNodes(adminMode)) {
                    suggestData.addElement(suggests);
                }
            }
        }

        // syntax比較
        if (currentNode != null) {
            if (currentNode.getSyntax() != null) {
                if (currentNode.compersionSyntax(currentToken))
                    suggestData.addElement(currentNode.getUseage());
            }
        }

        if (!suggestData.getElements().isEmpty()) {
            showChatBoxList(suggestData);
        }
    }

    /**
     * 現在のシーンで利用できるかどうかのチェック
     * 
     * @param allowedTypes シーンタイプ
     * @return 許可されていればtrue、されていなければfalse
     */
    private boolean isSceneValid(SceneType... allowedTypes) {
        SceneType current = SM.getSceneName();
        for (SceneType type : allowedTypes) {
            if (current == type)
                return true;
        }
        logText = "Command not available in the current scene.";
        logHistoryData.addElement(logText);
        LogUtils.debugLog(logText);
        return false;
    }

    /**
     * コマンドの登録
     */
    private void registCommands() {
        // fps変更 /fps <num>
        register(rootCommands, "/fps", "Change fps.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                Integer fps = GameUtils.tryParse(args[0]);

                // fpsが0以下の場合はエラー
                if (fps == 0) {
                    logText = "FPS must be greater than 0.";
                    logHistoryData.addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }

                MAIN.setFps(fps);

                logText = "Current fps set to " + fps + ".";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknown param: '" + args[0] + "'";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<number>");

        // 管理者権限切替 /admin
        register(rootCommands, "/admin", "Toggle admin mode.", false, (node, args) -> {
            if (ShootingGameMain.getOnlineMode()) {
                // 引数が不適切な場合
                if (args.length != 1) {
                    logText = node.getUseage();
                    logHistoryData.addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }

                String password = args[0];
                if (!DataManager.authenticate("admin", password)) {
                    logText = "password is not true.";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            } else {
                // 引数が不適切な場合
                if (args.length != 0) {
                    logText = node.getUseage();
                    logHistoryData.addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }
            }

            adminMode = !adminMode;

            logText = "AdminMode " + (adminMode ? "enabled." : "disabled.");
            logHistoryData.addElement(logText);
            LogUtils.systemLog("[INFO]", component, ANSI.YELLOW + logText + ANSI.RESET);
        }).setSyntax(ShootingGameMain.getOnlineMode() ? "<password>" : "");

        // ヘルプ /help
        register(rootCommands, "/help", "Show available commands.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            showHelp();
        });

        // シーン読み込み /loadscene <sceneName> /loadscene game <stageId>
        CommandNode loadscene = register(rootCommands, "/loadscene", "Change scene.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                if (SHIP_M.getPlayer() != null)
                    SHIP_M.getPlayer().removeHandler();

                SM.changeScene(SceneType.valueOf(args[0].toUpperCase()));
            } catch (Exception e) {
                logText = "Unknown scene: '" + args[0] + "'";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            }
        });
        loadscene.setSyntax("<sceneName>");
        register(loadscene, "game", "Load game stage.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].replace("stage", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!adminMode) {
                    // ロックされていた場合は遷移しない
                    int clearedStage = DataManager.getUserData().getProgress().getClearedStage() + 1;
                    if (id > clearedStage) {
                        logText = "Can't play locked stage.";
                        logHistoryData.addElement(logText);
                        LogUtils.debugLog(logText);
                        return;
                    }
                }

                STAGE_M.getStage().setStageId(id);
                SM.changeScene(SceneType.GAME);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<stageId>");
        register(loadscene, "test", "Load test stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            SM.changeScene(SceneType.TEST);
        });

        // リソースの読み込み直し /reload
        register(rootCommands, "/reload", "Reload resource", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            reload();
        });

        // 殺害 /kill <target>
        register(rootCommands, "/kill", "Kill targets.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            SHIP_M.kill(args[0]);

            logText = args[0] + " killed.";
            logHistoryData.addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        }).setSyntax("<target>");

        // 消去 /clear <target>
        register(rootCommands, "/clear", "Kill targets.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            SHIP_M.clear(args[0]);

            logText = args[0] + " cleared.";
            logHistoryData.addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        }).setSyntax("<target>");

        // 船タイプの変更 /setship
        register(rootCommands, "/setship", "Set now ship.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                    return;

                SHIP_M.getPlayer().setShipId("Ship" + id);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId>");

        // 船の解法 /unlockship
        register(rootCommands, "/unlockship", "Unlock ship.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = GameUtils.tryParse(args[0].replace("ship", ""));

                DataManager.getUserData().unlockShip("Ship" + id);
                LobbyManager.getInstance().updateButtonStates();
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId>");

        // 効果付与 /effect
        CommandNode effect = register(rootCommands, "/effect", "Apply effect.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length == 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
        });
        effect.setSyntax("<effectName>", "<seconds>");
        // 回復
        register(effect, "heal", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            try {
                Integer num = args.length > 0 ? GameUtils.tryParse(args[0]) : null;

                SHIP_M.getPlayer().heal(num);

                logText = "Healed " + num + "HP.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<seconds>");
        // レベルアップ
        register(effect, "levelup", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            // 指定がなければ1レベル上げる、指定があればその回数レベルアップ
            int amount = 1;
            try {
                Integer num = GameUtils.tryParse(args[0]);
                amount = num > 0 ? num : 1;
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            }
            // レベルアップ処理
            for (int i = 0; i < amount; i++) {
                SHIP_M.getPlayer().levelUp();
            }

            logText = "Ship leveled up to level " + SHIP_M.getPlayer().getShipLevel() + ".";
            logHistoryData.addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        });
        // レベル設定
        register(effect, "setLevel", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            try {
                Integer num = GameUtils.tryParse(args[0]);

                if (num >= 5) {
                    SHIP_M.getPlayer().setShipLevel(4);
                    SHIP_M.getPlayer().levelUp();
                } else
                    SHIP_M.getPlayer().setShipLevel(num);

                logText = "Ship level set to " + SHIP_M.getPlayer().getShipLevel() + ".";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<amount>");
        // 暴走
        register(effect, "ooc", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            boolean isInfinityOOC = SHIP_M.getPlayer().getOOCTimer() == Double.POSITIVE_INFINITY;
            // 指定がなければ暴走モードのトグル、指定があれば暴走時間を設定
            if (args.length == 0) {
                double time = isInfinityOOC ? 0 : Double.POSITIVE_INFINITY;
                int level = isInfinityOOC ? 4 : 5;
                isInfinityOOC = !isInfinityOOC;

                SHIP_M.getPlayer().setOOCTimer(time);
                SHIP_M.getPlayer().setShipLevel(level);

                logText = "OOC " + (isInfinityOOC ? "enabled." : "disabled.");
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                try {
                    Integer time = GameUtils.tryParse(args[0]);

                    if (isInfinityOOC) {
                        logText = "The ship is already OOC.";
                        logHistoryData.addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    } else {
                        SHIP_M.getPlayer().setShipLevel(5);
                        SHIP_M.getPlayer().setOOCTimer(time);

                        logText = "The ship became OOC.";
                        logHistoryData.addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    }
                } catch (NumberFormatException ne) {
                    logText = "Unknnown param: " + args[0];
                    logHistoryData.addElement(logText);
                    LogUtils.debugLog(logText);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        }).setSyntax("<seconds>");

        // 資源入手 /give
        register(rootCommands, "/get", "get resource", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 2) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                // 指定値の設定
                int openBraceIdx = args[0].indexOf("{");
                int closeBraceIdx = args[0].indexOf("}");

                String nameStr = args[0];
                Rarity rarity = Rarity.COMMON; // デフォルトはコモン
                if (openBraceIdx != -1 && closeBraceIdx != -1 && openBraceIdx < closeBraceIdx) {
                    // {の前を切り出す
                    nameStr = args[0].substring(0, openBraceIdx);

                    // {}の中身を取り出す
                    String contentInside = args[0].substring(openBraceIdx + 1, closeBraceIdx);

                    if (contentInside.startsWith("rarity=")) {
                        rarity = Rarity.valueOf(contentInside.substring(7).toUpperCase());
                    }
                }

                ResourceKinds resKind = new ResourceKinds(ResourceName.valueOf(nameStr.toUpperCase()), rarity);
                Integer amount = GameUtils.tryParse(args[1]);
                Resources resData = DataManager.getUserData().getResources();
                if (resKind == ResourceKinds.COIN) {
                    int amountOwned = resData.getCoins();
                    resData.setCoins(amountOwned + amount);

                    logText = "Added " + amount + " coins. Current coins: " + resData.getCoins() + ".";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                } else {
                    int amountOwned = resData.getEquipments().getOrDefault(resKind, 0);
                    int newAmount = amountOwned + amount;
                    if (newAmount <= 0) {
                        resData.getEquipments().remove(resKind);
                    } else {
                        resData.getEquipments().put(resKind, newAmount);
                    }
                }
                BagManager.getInstance().setup();

            } catch (NumberFormatException ne) {
                logText = "Unknown param: '" + args[1] + "'";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (IllegalArgumentException iae) {
                logText = "Unknown resName: '" + args[0] + "'";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<resourceName>", "<amount>");

        // ゲームクリア/ゲームオーバー /gameclear /gameover
        register(rootCommands, "/gameclear", "Instantly clear.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(false);
            GM.result();
        });
        register(rootCommands, "/gameover", "Instantly gameover.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(true);
            GM.result();
        });
        register(rootCommands, "/clearstage", "Force clear a specific stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                Integer stageId = GameUtils.tryParse(args[0]);

                UserData data = DataManager.getUserData();
                // 与えられたステージがクリア済みかどうか
                boolean isCleared = stageId <= data.getProgress().getClearedStage();
                if (!isCleared) {
                    data.getProgress().setClearedStage(stageId);
                    NSM.getStageButtonPanel().setup();

                    // クリア進捗更新ログ
                    logText = "Progress reached " + ANSI.YELLOW + "Stage" + stageId + ANSI.RESET + ".";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                } else {
                    logText = "Specified stage has already been completed.";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                }
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<stageId>");

        // データリセット /deleteuser
        register(rootCommands, "/deleteuser", "Reset data.", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                logText = "This command is unavailable in offline mode.";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            // 引数が不適切な場合
            if (args.length != 2) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String username = args[0];
            String password = args[1];

            // adminアカウントの削除防止
            if (username.equals("admin")) {
                logText = "Can't delete admin account.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[WARN]", component, logText);
                return;
            }

            // forceオプションが渡された場合強制削除
            if (password.equals("force")) {
                if (DataManager.forceDelete(username)) {
                    logText = "Delete successful.";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            }

            // 通常モードでデータ削除
            if (DataManager.delete(username, password)) {
                logText = "Delete successfull.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                logText = "Deletion failed.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[WARN]", component, logText);
            }
        }).setSyntax("<username>", "<password>");

        // パスワード変更 /changepassword
        register(rootCommands, "/changepassword", "Change user password", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                logText = "This command is unavailable in offline mode.";
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            // 引数が不適切な場合
            if (args.length != 3) {
                logText = node.getUseage();
                logHistoryData.addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String username = args[0];
            String oldPassword = args[1];
            String newPassword = args[2];

            // forceオプションが渡された場合パスワード強制変更
            if (oldPassword.equals("force")) {
                if (DataManager.forceChangePassword(username, newPassword)) {
                    logText = "Change password successful.";
                    logHistoryData.addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            }

            // 通常モードでパスワード変更
            if (DataManager.changePassword(username, oldPassword, newPassword)) {
                logText = "Change password successful.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                logText = "Change password failed.";
                logHistoryData.addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            }
        }).setSyntax("<username>", "<oldPassword>", "<newPassword>");
    }

    /**
     * ヘルプコマンドによる出力
     */
    private void showHelp() {
        LogUtils.debugLog("");

        logHistoryData.addElement("----------------- Available Commands -----------------");
        rootCommands.entrySet().stream()
                .filter(e -> !e.getValue().isAdminOnly() || adminMode) // 権限があるもの
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> logHistoryData.addElement(e.getKey() + " : " + e.getValue().getDescription()));
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
    private void reload() {
        // リロード開始ログ
        logText = "Attempting to reload resources...";
        logHistoryData.addElement(logText);
        LogUtils.systemLog("[INFO]", component, logText);

        RM.reload();
        IM.reload();
        SHIP_M.reload();
        STAGE_M.reload();
        BM.reload();
        NSM.reload();
        UM.reload();
        // テストシーンの場合敵をクリアして再生成
        if (SM.getSceneName() == SceneType.TEST)
            SHIP_M.clear("all");

        // リロード完了ログ
        logText = "Reload complete.";
        logHistoryData.addElement(logText);
        LogUtils.systemLog("[INFO]", component, logText);
    }

    /**
     * 未知コマンドに対するログ
     * 
     * @param text 未知コマンド内容
     */
    private void unknownCommand(String text) {
        logText = "Unknown command '" + text + "'";
        logHistoryData.addElement(logText);
        LogUtils.debugLog(logText);
        logText = "Type '/help' for help.";
        logHistoryData.addElement(logText);
        LogUtils.debugLog(logText);
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