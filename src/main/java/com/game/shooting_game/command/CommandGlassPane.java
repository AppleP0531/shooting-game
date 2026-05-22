package com.game.shooting_game.command;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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

import lombok.Setter;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
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
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public class CommandGlassPane extends JPanel {
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

    @FunctionalInterface
    interface CommandAction {
        void execute(CommandNode node, String[] args);
    }

    @Setter
    private boolean adminMode = false;

    private Map<String, CommandNode> rootCommands = new HashMap<>();

    private final JTextField commandField; // コマンド入力テキストフィールド

    // 入力履歴
    private final List<String> COMMAND_INPUT_HISTORY = new LinkedList<>(); // 入力履歴リスト
    private final DefaultListModel<String> INPUT_HISTORY_MODEL; // 入力履歴表示用モデル
    private final JList<String> INPUT_HISTORY_LIST; // 入力履歴表示用リスト
    private final int MAX_INPUT_HISTORY = 30; // 入力履歴の最大保持数
    private final int MAX_INPUT_HISTORY_DISPLAY = 10; // 入力履歴の表示最大数
    private String currentInput = ""; // 現在の入力内容
    private int inputHistoryIndex = -1; // 入力履歴参照用インデックス
    private int inputHistoryDiff = 0; // 入力履歴リストの表示差分(表示最大数以上の回数さかのぼった時用)
    private ChatBoxData inputHistoryData;

    // ログ
    private final List<String> COMMAND_LOG_HISTORY = new LinkedList<>(); // コマンドログ履歴リスト
    private final DefaultListModel<String> LOG_HISTORY_MODEL; // コマンドログ表示用モデル
    private final JList<String> LOG_HISTORY_LIST; // コマンドログ表示用リスト
    private final int MAX_LOG_HISTORY = 30; // コマンドログの最大保持数
    private final int MAX_LOG_HISTORY_DISPLAY = 10; // コマンドログの表示最大数
    private int logHistoryIndex = -1; // コマンドログ参照用インデックス
    private int logHistoryDiff = 0; // コマンドログリストの表示差分
    private ChatBoxData logHistoryData;

    // 予測変換

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
        Vector2 textBoxSize = new Vector2(MAIN.getWidth() - 20, 30 * ShootingGameMain.getScreenSizeAdj());
        commandField = new JTextField();
        commandField.setBounds(10, MAIN.getHeight() - 10 - (int) textBoxSize.getY(),
                (int) textBoxSize.getX(), (int) textBoxSize.getY());
        commandField.setBackground(new Color(0, 0, 0, 200));
        commandField.setForeground(Color.WHITE);
        commandField.setCaretColor(Color.WHITE);
        commandField.setFont(new Font("Monospaced", Font.BOLD, (int) (16 * ShootingGameMain.getScreenSizeAdj())));
        commandField.setBorder(BorderFactory.createLineBorder(Color.CYAN));
        commandField.setVisible(false);

        // コマンド入力時の処理
        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_UP -> {
                        moveHistory(inputHistoryData, -1);
                    }
                    case KeyEvent.VK_DOWN -> {
                        moveHistory(inputHistoryData, 1);
                    }
                    case KeyEvent.VK_ENTER -> executeCommand();
                    case KeyEvent.VK_ESCAPE -> {
                        hideCommandBar();
                        e.consume();
                    }
                    default -> hideHistoryList(inputHistoryData);
                }
            }
        });
        commandField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getPreciseWheelRotation() < 0) {
                    moveHistory(logHistoryData, -1);
                } else {
                    moveHistory(logHistoryData, 1);
                }
            }
        });
        add(commandField);

        // 履歴リストの設定
        INPUT_HISTORY_MODEL = new DefaultListModel<>();
        INPUT_HISTORY_LIST = new JList<>(INPUT_HISTORY_MODEL);
        INPUT_HISTORY_LIST.setBackground(new Color(0, 0, 0, 180));
        INPUT_HISTORY_LIST.setForeground(Color.LIGHT_GRAY);
        INPUT_HISTORY_LIST
                .setFont(new Font("Monospaced", Font.PLAIN, (int) (14 * ShootingGameMain.getScreenSizeAdj())));
        INPUT_HISTORY_LIST.setBorder(null);
        INPUT_HISTORY_LIST.setVisible(false);
        INPUT_HISTORY_LIST.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                // 基本的な描画設定（テキストや背景色など）を継承
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
        inputHistoryData = new ChatBoxData(
                COMMAND_INPUT_HISTORY,
                INPUT_HISTORY_MODEL,
                INPUT_HISTORY_LIST,
                MAX_INPUT_HISTORY,
                MAX_INPUT_HISTORY_DISPLAY,
                inputHistoryIndex,
                inputHistoryDiff);

        // ログリストの設定
        LOG_HISTORY_MODEL = new DefaultListModel<>();
        LOG_HISTORY_LIST = new JList<>(LOG_HISTORY_MODEL);
        LOG_HISTORY_LIST.setBackground(new Color(0, 0, 0, 100));
        LOG_HISTORY_LIST.setForeground(Color.LIGHT_GRAY);
        LOG_HISTORY_LIST.setFont(new Font("Monospaced", Font.PLAIN, (int) (10 * ShootingGameMain.getScreenSizeAdj())));
        LOG_HISTORY_LIST.setBorder(null);
        LOG_HISTORY_LIST.setVisible(false);
        LOG_HISTORY_LIST.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                // 基本的な描画設定（テキストや背景色など）を継承
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
        add(LOG_HISTORY_LIST);
        logHistoryData = new ChatBoxData(
                COMMAND_LOG_HISTORY,
                LOG_HISTORY_MODEL,
                LOG_HISTORY_LIST,
                MAX_LOG_HISTORY,
                MAX_LOG_HISTORY_DISPLAY,
                logHistoryIndex,
                logHistoryDiff);

        // 入力中にマウスイベントをほかにとられないようにする
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // commandField の範囲外をクリックしたか判定
                if (!commandField.getBounds().contains(e.getPoint())) {
                    hideCommandBar();
                }
            }
        });

        registCommands();
    }

    /**
     * 履歴の移動
     * 
     * @param data チャットボックス情報
     * @param diff 移動量
     * @return 次の参照インデックス
     */
    private void moveHistory(ChatBoxData data, int diff) {
        if (data.getHistory().isEmpty())
            return;

        int displayListSize = Math.min(data.getMaxHistoryDisplay(), data.getHistory().size());

        // 履歴移動処理
        if (diff == -1) {
            if (data.getHistoryIndex() == -1) {
                // 現在の入力を保存
                currentInput = commandField.getText();
                data.setHistoryIndex(displayListSize - 1);
            } else {
                // 表示履歴の一番上に到達したら1行上にスクロールする
                if (data.getHistoryIndex() == 0 && data.getHistory().size() >= data.getMaxHistoryDisplay())
                    data.setHistoryDiff(Math.min(data.getHistoryDiff() + 1,
                            data.getHistory().size() - data.getMaxHistoryDisplay()));

                data.setHistoryIndex(Math.max(0, data.getHistoryIndex() - 1));
            }
        } else {
            if (data.getHistoryIndex() == -1)
                return;

            if (data.getHistoryDiff() != 0 || data.getHistoryIndex() < displayListSize - 1) {
                // 表示履歴の一番下に到達したら一行下にスクロールする
                if (data.getHistoryIndex() >= data.getMaxHistoryDisplay() - 1)
                    data.setHistoryDiff(Math.max(0, data.getHistoryDiff() - 1));

                data.setHistoryIndex(Math.min(data.getMaxHistoryDisplay() - 1, data.getHistoryIndex() + 1));
            } else
                data.setHistoryIndex(-1);
        }

        // UIへの反映
        showHistoryList(data);
        data.getHistoryList().setSelectedIndex(data.getHistoryIndex());
        commandField.setText(data.getHistoryIndex() == -1 ? currentInput
                : data.getHistory().get(data.getHistory().size() - 1
                        - (displayListSize - 1 - data.getHistoryIndex()) - data.getHistoryDiff()));
    }

    /**
     * コマンドウィンドウの表示
     */
    public void showCommandBar() {
        setVisible(true);
        commandField.setVisible(true);
        commandField.setText("/");
        commandField.requestFocusInWindow();

        showHistoryList(logHistoryData);
    }

    /**
     * コマンドウィンドウの非表示
     */
    public void hideCommandBar() {
        setVisible(false);
        commandField.setVisible(false);

        hideHistoryList(logHistoryData);
    }

    /**
     * 履歴リストの表示
     * 
     * @param data チャットボックス情報
     */
    private void showHistoryList(ChatBoxData data) {
        // データがなければ表示しない
        if (data.getHistory().size() == 0)
            return;

        data.getHistoryModel().clear();

        int start = Math.max(0, data.getHistory().size() - data.getHistoryDiff() - data.getMaxHistoryDisplay());
        for (int i = start; i < data.getHistory().size() - data.getHistoryDiff(); i++) {
            data.getHistoryModel().addElement(data.getHistory().get(i));
        }

        // リスト全体の高さを計算
        int rowHeight = 20; // 1行あたりの高さ
        data.getHistoryList().setFixedCellHeight(rowHeight);

        // 配置の計算と設定
        repaint();
        int listHeight = (data.getHistoryModel().size() * rowHeight);
        data.getHistoryList().setBounds(
                commandField.getX(),
                commandField.getY() - listHeight - 1,
                commandField.getWidth(),
                listHeight);

        data.getHistoryList().setVisible(true);
    }

    /**
     * 履歴リストの非表示
     * 
     * @param data チャットボックス情報
     */
    private void hideHistoryList(ChatBoxData data) {
        data.getHistoryList().setVisible(false);
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
        inputHistoryData.setHistoryIndex(-1);
        logHistoryData.setHistoryIndex(-1);
        currentInput = "";
        hideCommandBar();
        hideHistoryList(inputHistoryData);
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
        logHistoryData.addElement("Command not available in the current scene.");
        LogUtils.debugLog("Command not available in the current scene.");
        return false;
    }

    /**
     * コマンドの登録
     */
    private void registCommands() {
        // fps変更 /fps <num>
        register(rootCommands, "/fps", "Change fps.", false, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<number>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            try {
                Integer fps = tryParse(args[0]);

                // fpsが0以下の場合はエラー
                if (fps == 0) {
                    logHistoryData.addElement("FPS must be greater than 0.");
                    LogUtils.debugLog("FPS must be greater than 0.");
                    return;
                }

                MAIN.setFps(fps);
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Current fps set to " + fps);
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("unknown param: " + args[0]);
                LogUtils.debugLog("unknown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 管理者権限切替 /admin
        register(rootCommands, "/admin", "Toggle admin mode.", false, (node, args) -> {
            if (ShootingGameMain.getOnlineMode()) {
                // 引数のヒント
                node.setSyntax("<password>");

                // 引数が不適切な場合
                if (args.length != 1) {
                    logHistoryData.addElement(node.getUseage());
                    LogUtils.debugLog(node.getUseage());
                    return;
                }

                String password = args[0];
                if (!DataManager.authenticate("admin", password)) {
                    LogUtils.systemLog("[INFO]", component, "password is not true");
                    return;
                }
            } else {
                // 引数が不適切な場合
                if (args.length != 0) {
                    logHistoryData.addElement(node.getUseage());
                    LogUtils.debugLog(node.getUseage());
                    return;
                }
            }

            adminMode = !adminMode;
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.YELLOW + "AdminMode " + (adminMode ? "enabled." : "disabled.") + ANSI.RESET);
        });

        // ヘルプ /help
        register(rootCommands, "/help", "Show available commands.", false, (node, args) -> {

            // 引数が不適切な場合
            if (args.length != 0) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            showHelp();
        });

        // シーン読み込み /loadscene <sceneName> /loadscene game <stageId>
        CommandNode loadscene = register(rootCommands, "/loadscene", "Change scene.", false, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<sceneName>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            try {
                if (SHIP_M.getPlayer() != null)
                    SHIP_M.getPlayer().removeHandler();

                SM.changeScene(SceneType.valueOf(args[0].toUpperCase()));
            } catch (Exception e) {
                logHistoryData.addElement("Unknown scene: '" + args[0] + "'" + e);
                LogUtils.debugLog("Unknown scene: '" + args[0] + "'" + e);
            }
        });
        register(loadscene, "game", "Load game stage.", false, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<stageId>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            String param = args[0].replace("stage", "");
            try {
                Integer id = tryParse(param);

                if (!adminMode) {
                    // ロックされていた場合は遷移しない
                    int clearedStage = DataManager.getUserData().getProgress().getClearedStage() + 1;
                    if (id > clearedStage) {
                        logHistoryData.addElement("Can't play locked stage");
                        LogUtils.debugLog("Can't play locked stage");
                        return;
                    }
                }

                STAGE_M.getStage().setStageId(id);
                SM.changeScene(SceneType.GAME);
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + param);
                LogUtils.debugLog("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });
        register(loadscene, "test", "Load test stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            SM.changeScene(SceneType.TEST);
        });

        // リソースの読み込み直し /reload
        register(rootCommands, "/reload", "Reload resource", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            reload();
        });

        // 殺害 /kill <target>
        register(rootCommands, "/kill", "Kill targets.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<target>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;
            SHIP_M.kill(args[0]);
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    args[0] + " killed.");
        });

        // 消去 /clear <target>
        register(rootCommands, "/clear", "Kill targets.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<target>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            SHIP_M.clear(args[0]);
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    args[0] + " cleared.");
        });

        // 船タイプの変更 /setship
        register(rootCommands, "/setship", "Set now ship.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<shipId>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = tryParse(param);

                if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                    return;

                SHIP_M.getPlayer().setShipId("Ship" + id);
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + param);
                LogUtils.debugLog("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 船の解法 /unlockship
        register(rootCommands, "/unlockship", "Unlock ship.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<shipId>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = tryParse(args[0].replace("ship", ""));

                DataManager.getUserData().unlockShip("Ship" + id);
                LobbyManager.getInstance().updateButtonStates();
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + param);
                LogUtils.debugLog("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 効果付与 /effect
        CommandNode effect = register(rootCommands, "/effect", "Apply effect.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<effectName> <seconds>");

            // 引数が不適切な場合
            if (args.length == 0) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
        });
        // 回復
        register(effect, "heal", null, true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<seconds>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            try {
                Integer num = args.length > 0 ? tryParse(args[0]) : null;

                SHIP_M.getPlayer().heal(num);
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Healed " + num + "HP.");
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + args[0]);
                LogUtils.debugLog("Unknnown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });
        // レベルアップ
        register(effect, "levelup", null, true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<amount>");

            // 引数が不適切な場合
            if (args.length > 2) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            // 指定がなければ1レベル上げる、指定があればそのレベルを設定
            if (args.length == 0) {
                SHIP_M.getPlayer().levelUp();
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Ship leveled up to level " + SHIP_M.getPlayer().getShipLevel() + ".");
            } else {
                try {
                    Integer num = tryParse(args[0]);

                    if (num >= 5) {
                        SHIP_M.getPlayer().setShipLevel(4);
                        SHIP_M.getPlayer().levelUp();
                    } else
                        SHIP_M.getPlayer().setShipLevel(num);

                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Ship level set to " + SHIP_M.getPlayer().getShipLevel());
                } catch (NumberFormatException ne) {
                    logHistoryData.addElement("Unknnown param: " + args[0]);
                    LogUtils.debugLog("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        });
        // 無敵
        register(effect, "shield", null, true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<seconds>");

            // 引数が不適切な場合
            if (args.length > 2) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            boolean isInfinityInvincibility = SHIP_M.getPlayer().getShieldTimer() == Double.POSITIVE_INFINITY;
            // 指定がなければ無敵モードのトグル、指定があれば無敵時間を設定
            if (args.length == 0) {
                double time = isInfinityInvincibility ? 0 : Double.POSITIVE_INFINITY;
                isInfinityInvincibility = !isInfinityInvincibility;

                SHIP_M.getPlayer().setShieldTimer(time);
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Invincibility " + (isInfinityInvincibility ? "enabled." : "disabled."));
            } else {
                try {
                    Integer time = tryParse(args[0]);

                    if (isInfinityInvincibility) {
                        LogUtils.systemLog(
                                "[INFO]",
                                component,
                                "The ship is already invincible.");
                    } else
                        SHIP_M.getPlayer().setShieldTimer(time);
                } catch (NumberFormatException ne) {
                    logHistoryData.addElement("Unknnown param: " + args[0]);
                    LogUtils.debugLog("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        });
        // 暴走
        register(effect, "ooc", null, true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<seconds>");

            // 引数が不適切な場合
            if (args.length > 2) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
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
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "OOC " + (isInfinityOOC ? "enabled." : "disabled."));
            } else {
                try {
                    Integer time = tryParse(args[0]);

                    if (isInfinityOOC) {
                        LogUtils.systemLog(
                                "[INFO]",
                                component,
                                "The ship is already OOC");
                    } else {

                        SHIP_M.getPlayer().setOOCTimer(time);
                    }
                } catch (NumberFormatException ne) {
                    logHistoryData.addElement("Unknnown param: " + args[0]);
                    LogUtils.debugLog("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        });

        // 資源入手 /give
        register(rootCommands, "/get", "get resource", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<resourceName> <amount>");

            // 引数が不適切な場合
            if (args.length != 2) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            try {
                ResourceKinds resName = ResourceKinds.valueOf(args[0].toUpperCase());
                Integer amount = tryParse(args[1]);
                Resources resData = DataManager.getUserData().getResources();
                if (resName == ResourceKinds.COIN) {
                    int amountOwned = resData.getCoins();
                    resData.setCoins(amountOwned + amount);
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Added " + amount + " coins. Current coins: " + resData.getCoins() + ".");
                    return;
                } else {
                    int amountOwned = resData.getEquipments().getOrDefault(resName, 0);
                    int newAmount = amountOwned + amount;
                    if (newAmount <= 0) {
                        resData.getEquipments().remove(resName);
                    } else {
                        resData.getEquipments().put(resName, newAmount);
                    }
                }

            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + args[1]);
                LogUtils.debugLog("Unknnown param: " + args[1]);
            } catch (IllegalArgumentException iae) {
                logHistoryData.addElement("Unknown resName: " + args[0]);
                LogUtils.debugLog("Unknown resName: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // ゲームクリア/ゲームオーバー /gameclear /gameover
        register(rootCommands, "/gameclear", "Instantly clear.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
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
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(true);
            GM.result();
        });
        register(rootCommands, "/clearstage", "Force clear a specific stage.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<stageId>");

            // 引数が不適切な場合
            if (args.length != 1) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            try {
                Integer stageId = tryParse(args[0]);

                UserData data = DataManager.getUserData();
                // 与えられたステージがクリア済みかどうか
                boolean isCleared = stageId <= data.getProgress().getClearedStage();
                if (!isCleared) {
                    data.getProgress().setClearedStage(stageId);
                    NSM.getStageButtonPanel().setup();

                    // クリア進捗更新ログ
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Progress reached " + ANSI.YELLOW + "Stage" + stageId + ANSI.RESET + ".");
                } else {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Specified stage has already been completed.");
                }
            } catch (NumberFormatException ne) {
                logHistoryData.addElement("Unknnown param: " + args[0]);
                LogUtils.debugLog("Unknnown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // データリセット /deleteuser
        register(rootCommands, "/deleteuser", "Reset data.", true, (node, args) -> {
            // 引数のヒント
            node.setSyntax("<username> <password>");

            // 引数が不適切な場合
            if (args.length != 2) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            String username = args[0];
            String password = args[1];

            // adminアカウントの削除防止
            if (username.equals("admin")) {
                LogUtils.systemLog(
                        "[WARN]",
                        component,
                        "Can't delete admin account");
                return;
            }

            // forceオプションが渡された場合強制削除
            if (password.equals("force")) {
                if (DataManager.forceDelete(username)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Delete successful");
                    return;
                }
            }

            // 通常モードでデータ削除
            if (DataManager.delete(username, password))
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Delete successfull");
            else
                LogUtils.systemLog(
                        "[WARN]",
                        component,
                        "Deletion failed");
        });

        // パスワード変更 /changepassword
        register(rootCommands, "/changepassword", "Change user password", true, (node, args) -> {
            node.setSyntax("<username> <oldPassword> <newPassword>");

            // 引数が不適切な場合
            if (args.length != 3) {
                logHistoryData.addElement(node.getUseage());
                LogUtils.debugLog(node.getUseage());
                return;
            }

            String username = args[0];
            String oldPassword = args[1];
            String newPassword = args[2];

            // forceオプションが渡された場合パスワード強制変更
            if (oldPassword.equals("force")) {
                if (DataManager.forceChangePassword(username, newPassword)) {
                    LogUtils.systemLog(
                            "[INFO]",
                            component,
                            "Change password successful");
                    return;
                }
            }

            // 通常モードでパスワード変更
            if (DataManager.changePassword(username, oldPassword, newPassword))
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Change password successful");
            else
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Change password failed");
        });
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
     * 数字に変換
     * 
     * @param value 値
     * @return 変換できた場合は値、できなかった場合はnull
     */
    private Integer tryParse(String value) throws NumberFormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw e;
        }
    }

    /**
     * リソースの再読み込み
     */
    private void reload() {
        // リロード開始ログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Attempting to reload resources...");
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
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Reload complete.");
    }

    /**
     * 未知コマンドに対するログ
     * 
     * @param text 未知コマンド内容
     */
    private void unknownCommand(String text) {
        logHistoryData.addElement("Unknown command '" + text + "'");
        logHistoryData.addElement("Type '/help' for help.");
        LogUtils.debugLog("Unknown command '" + text + "'");
        LogUtils.debugLog("Type '/help' for help.");
    }
}