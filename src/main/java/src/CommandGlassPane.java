package src;

import src.SceneManager.SceneType;
import src.ResourceManager.ResourceKinds;
import src.data.DataManager;
import src.data.DataManager.UserData;
import src.data.DataManager.UserData.Resources;
import src.utils.ANSI;
import src.utils.LogUtils;
import src.gameScene.GameManager;
import src.gameScene.item.ItemManager;
import src.gameScene.ship.ShipManager;
import src.gameScene.stage.StageManager;
import src.lobbyScene.LobbyManager;
import src.stageScene.normal.NormalStageManager;
import src.gameScene.bullet.BulletManager;
import src.upgradeScene.UpgradeManager;
import src.utils.Vector2;

import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import lombok.Data;
import lombok.Setter;

public class CommandGlassPane extends JPanel {
    private static final String component = "[Command] ";

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
        void execute(String[] args);
    }

    /**
     * コマンドノード
     */
    @Data // Getter, Setterの自動生成
    private static class CommandNode {
        private String description;
        private boolean adminOnly;
        private CommandAction action;
        private Map<String, CommandNode> subCommands = new HashMap<>();

        public CommandNode(String description, boolean adminOnly, CommandAction action) {
            this.description = description;
            this.adminOnly = adminOnly;
            this.action = action;
        }
    }

    private final Map<String, CommandNode> rootCommands = new HashMap<>();

    private final JTextField commandField; // コマンド入力テキストフィールド

    // 入力履歴
    private final List<String> commandInputHistory = new LinkedList<>(); // 入力履歴リスト
    private final DefaultListModel<String> inputHistoryModel; // 入力履歴表示用モデル
    private final int MAX_INPUT_HISTORY = 30; // 入力履歴の最大保持数
    private final int MAX_INPUT_HISTORY_DISPLAY = 10; // 入力履歴の表示最大数
    private final JList<String> inputHistoryList; // 入力履歴表示用リスト
    private String currentInput = ""; // 現在の入力内容
    private int inputHistoryIndex = -1; // 入力履歴参照用インデックス
    private int inputHistoryDiff = 0; // 入力履歴リストの表示差分(表示最大数以上の回数さかのぼった時用)

    @Setter
    private boolean adminMode = false;

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
                    case KeyEvent.VK_UP -> moveHistory(-1);
                    case KeyEvent.VK_DOWN -> moveHistory(1);
                    case KeyEvent.VK_ENTER -> executeCommand();
                    case KeyEvent.VK_ESCAPE -> {
                        hideCommandBar();
                        e.consume();
                    }
                    default -> hideHistoryList();
                }
            }
        });

        add(commandField);

        // 履歴リストの設定
        inputHistoryModel = new DefaultListModel<>();
        inputHistoryList = new JList<>(inputHistoryModel);
        inputHistoryList.setBackground(new Color(0, 0, 0, 180));
        inputHistoryList.setForeground(Color.LIGHT_GRAY);
        inputHistoryList.setFont(new Font("Monospaced", Font.PLAIN, (int) (14 * ShootingGameMain.getScreenSizeAdj())));
        inputHistoryList.setBorder(null);
        inputHistoryList.setVisible(false);
        add(inputHistoryList);

        inputHistoryList.setCellRenderer(new DefaultListCellRenderer() {
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
     * @param diff 移動量
     */
    private void moveHistory(int diff) {
        if (commandInputHistory.isEmpty())
            return;

        int displayListSize = Math.min(MAX_INPUT_HISTORY_DISPLAY, commandInputHistory.size());

        // 履歴移動処理
        if (diff == -1) {
            if (inputHistoryIndex == -1) {
                // 現在の入力を保存
                currentInput = commandField.getText();
                inputHistoryIndex = displayListSize - 1;
            } else {
                // 表示履歴の一番上に到達したら1行上にスクロールする
                if (inputHistoryIndex == 0 && commandInputHistory.size() >= MAX_INPUT_HISTORY_DISPLAY)
                    inputHistoryDiff = Math.min(inputHistoryDiff + 1,
                            commandInputHistory.size() - MAX_INPUT_HISTORY_DISPLAY);

                inputHistoryIndex = Math.max(0, inputHistoryIndex - 1);
            }
        } else {
            if (inputHistoryIndex == -1)
                return;

            if (inputHistoryDiff != 0 || inputHistoryIndex < displayListSize - 1) {
                // 表示履歴の一番下に到達したら一行下にスクロールする
                if (inputHistoryIndex >= MAX_INPUT_HISTORY_DISPLAY - 1)
                    inputHistoryDiff = Math.max(0, inputHistoryDiff - 1);

                inputHistoryIndex = Math.min(MAX_INPUT_HISTORY_DISPLAY - 1, inputHistoryIndex + 1);
            } else
                inputHistoryIndex = -1;

        }

        // UIへの反映
        showHistoryList(inputHistoryDiff);
        inputHistoryList.setSelectedIndex(inputHistoryIndex);
        commandField.setText(inputHistoryIndex == -1 ? currentInput
                : commandInputHistory.get(commandInputHistory.size() - 1
                        - (displayListSize - 1 - inputHistoryIndex) - inputHistoryDiff));
    }

    /**
     * 入力処理
     */
    private void executeCommand() {
        // 入力内容があれば処理、履歴に追加
        if (!commandField.getText().isEmpty()) {
            processCommand(commandField.getText());

            // 履歴に追加
            commandInputHistory.add(commandField.getText());
            if (commandInputHistory.size() > MAX_INPUT_HISTORY)
                commandInputHistory.removeFirst();
        }

        // リセット
        inputHistoryIndex = -1;
        currentInput = "";
        hideCommandBar();
        hideHistoryList();
    }

    /**
     * コマンドウィンドウの表示
     */
    public void showCommandBar() {
        setVisible(true);
        commandField.setVisible(true);
        commandField.setText("/");
        commandField.requestFocusInWindow();

        // logHistoryList.setVisible(true);
    }

    /**
     * コマンドウィンドウの非表示
     */
    public void hideCommandBar() {
        commandField.setVisible(false);
        // logHistoryList.setVisible(false);
        setVisible(false);
    }

    /**
     * 履歴リストの表示
     * 
     * @param diff 表示履歴のずらす量
     */
    private void showHistoryList(int diff) {
        inputHistoryModel.clear();

        int start = Math.max(0, commandInputHistory.size() - diff - MAX_INPUT_HISTORY_DISPLAY);
        for (int i = start; i < commandInputHistory.size() - diff; i++) {
            inputHistoryModel.addElement(commandInputHistory.get(i));
        }

        // リスト全体の高さを計算
        int rowHeight = 20; // 1行あたりの高さ
        inputHistoryList.setFixedCellHeight(rowHeight);

        // 配置の計算と設定
        repaint();
        int listHeight = (inputHistoryModel.size() * rowHeight);
        inputHistoryList.setBounds(
                commandField.getX(),
                commandField.getY() - listHeight - 1,
                commandField.getWidth(),
                listHeight);

        inputHistoryList.setVisible(true);
    }

    /**
     * 履歴リストの非表示
     */
    private void hideHistoryList() {
        inputHistoryList.setVisible(false);
    }

    /**
     * コマンド処理
     * 
     * @param text 入力内容
     */
    private void processCommand(String text) {
        // "/"から始まっていたらコマンドを処理、そうでなかったらログにそのまま出力
        if (!text.startsWith("/")) {
            System.out.println(text);
            return;
        }

        String[] parts = text.split(" ");
        String cmdName = parts[0];
        CommandNode node = rootCommands.get(cmdName);

        // コマンドリストになかった場合
        if (node == null) {
            unknownCommand(text);
            return;
        }

        // 権限不足
        if (node.adminOnly && !adminMode) {
            unknownCommand(text);
            return;
        }

        // 引数の切り出し
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        // サブコマンドの探索
        if (args.length > 0 && node.subCommands.containsKey(args[0])) {
            CommandNode sub = node.subCommands.get(args[0]);

            // サブコマンドの権限チェック
            if (sub.adminOnly && !adminMode) {
                unknownCommand(text);
                return;
            }

            sub.action.execute(Arrays.copyOfRange(args, 1, args.length));
        } else if (node.action != null) {
            node.action.execute(args);
        } else {
            unknownCommand(text);
        }
    }

    /**
     * ルートコマンド登録
     * 
     * @param name   コマンド名
     * @param desc   説明
     * @param admin  管理者権限が必要かどうか
     * @param action 処理
     * @return コマンドノード
     */
    private CommandNode register(String name, String desc, boolean admin, CommandAction action) {
        CommandNode node = new CommandNode(desc, admin, action);
        rootCommands.put(name, node);
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
        System.out.println("Command not available in the current scene.");
        return false;
    }

    /**
     * コマンドの登録
     */
    private void registCommands() {
        // fps変更 /fps <num>
        register("/fps", "Change fps.", false, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/fps <number>");
                return;
            }

            try {
                Integer fps = tryParse(args[0]);

                // fpsが0以下の場合はエラー
                if (fps == 0) {
                    System.out.println("FPS must be greater than 0.");
                    return;
                }

                MAIN.setFps(fps);
                LogUtils.systemLog(
                        "[INFO]",
                        component,
                        "Current fps set to " + fps);
            } catch (NumberFormatException ne) {
                System.out.println("unknown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 管理者権限切替 /admin
        register("/admin", "Toggle admin mode.", false, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/admin <password>");
                return;
            }

            String password = args[0];
            if (!DataManager.authenticate("admin", password)) {
                LogUtils.systemLog("[INFO]", component, "password is not true");
                return;
            }

            adminMode = !adminMode;
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    ANSI.YELLOW + "AdminMode " + (adminMode ? "enabled." : "disabled.") + ANSI.RESET);
        });

        // ヘルプ /help
        register("/help", "Show available commands.", false, args -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                System.out.println("/help");
                return;
            }

            showHelp();
        });

        // シーン読み込み /loadscene <sceneName> /loadscene game <stageId>
        CommandNode loadscene = register("/loadscene", "Change scene.", false, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/loadscene <sceneName>");
                return;
            }

            try {
                SHIP_M.getPlayer().removeHandler();
                SM.changeScene(SceneType.valueOf(args[0].toUpperCase()));
            } catch (Exception e) {
                System.out.println("Unknown scene: " + args[0]);
            }
        });
        loadscene.subCommands.put("game", new CommandNode("Load game stage.", false, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/loadscene game <stageId>");
                return;
            }

            String param = args[0].replace("stage", "");
            try {
                Integer id = tryParse(param);

                STAGE_M.getStage().setStageId(id);
                SM.changeScene(SceneType.GAME);
            } catch (NumberFormatException ne) {
                System.out.println("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }));
        loadscene.subCommands.put("test", new CommandNode("Load test stage.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                System.out.println("/loadscene <sceneName>");
                return;
            }

            SM.changeScene(SceneType.TEST);
        }));

        // リソースの読み込み直し /reload
        register("/reload", "Reload resource", false, args -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                System.out.println("/reload");
                return;
            }

            reload();
        });

        // 殺害 /kill <target>
        register("/kill", "Kill targets.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/kill <target>");
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
        register("/clear", "Kill targets.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/clear <target>");
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
        register("/setship", "Set ship type.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/setship <shipType>");
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = tryParse(param);

                if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                    return;

                SHIP_M.getPlayer().setShipId("Ship" + id);
            } catch (NumberFormatException ne) {
                System.out.println("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 船の解法 /unlockship
        register("/unlockship", "Unlock ship type.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/unlockship <shipType>");
                return;
            }

            String param = args[0].replace("ship", "");
            try {
                Integer id = tryParse(args[0].replace("ship", ""));

                DataManager.getUserData().unlockShip("Ship" + id);
                LobbyManager.getInstance().updateButtonStates();
            } catch (NumberFormatException ne) {
                System.out.println("Unknnown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // 効果付与 /effect
        CommandNode effect = register("/effect", "Apply effect.", true, null);
        // 回復
        effect.subCommands.put("heal", new CommandNode(null, true, args -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                System.out.println("/effect heal <number>");
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
                System.out.println("Unknnown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }));
        // レベルアップ
        effect.subCommands.put("levelup", new CommandNode(null, true, args -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                System.out.println("/effect levelup <number>");
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
                    System.out.println("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        }));
        // 無敵
        effect.subCommands.put("shield", new CommandNode(null, true, args -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                System.out.println("/effect shield <number>");
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
                    System.out.println("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        }));
        // 暴走
        effect.subCommands.put("ooc", new CommandNode(null, true, args -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                System.out.println("/effect ooc <number>");
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
                    System.out.println("Unknnown param: " + args[0]);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        }));

        // 資源入手 /give
        register("/get", "get resource", true, args -> {
            // 引数が不適切な場合
            if (args.length != 2) {
                System.out.println("/get <resource name> <ammount>");
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
                    int amountOwned = resData.getEquipment().getOrDefault(resName, 0);
                    int newAmount = amountOwned + amount;
                    if (newAmount <= 0) {
                        resData.getEquipment().remove(resName);
                    } else {
                        resData.getEquipment().put(resName, newAmount);
                    }
                }

            } catch (NumberFormatException ne) {
                System.out.println("Unknnown param: " + args[1]);
            } catch (IllegalArgumentException iae) {
                System.out.println("Unknown resName: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // ゲームクリア/ゲームオーバー /gameclear /gameover
        register("/gameclear", "Instantly clear.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                System.out.println("/gameclear");
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(false);
            GM.result();
        });
        register("/gameover", "Instantly gameover.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                System.out.println("/gameover");
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(true);
            GM.result();
        });
        register("/clearstage", "Force clear a specific stage.", true, args -> {
            if (args.length != 1) {
                System.out.println("/clearstage <number>");
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
                System.out.println("Unknnown param: " + args[0]);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        });

        // データリセット /deleteuser
        register("/deleteuser", "Reset data.", true, args -> {
            // 引数が不適切な場合
            if (args.length != 2) {
                System.out.println("/deleteuser <username> <password>");
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
        register("/changepassword", "Change user password", true, args -> {
            // 引数が不適切な場合
            if (args.length != 3) {
                System.out.println("/changepassword <username> <oldPassword> <newPassword>");
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
        System.out.println("");

        System.out.println(ANSI.YELLOW + "----------------- Available Commands -----------------" + ANSI.RESET);
        rootCommands.entrySet().stream()
                .filter(e -> !e.getValue().adminOnly || adminMode) // 権限があるもの
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("%-15s : %s%n", e.getKey(), e.getValue().getDescription()));

        System.out.println("");
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
        System.out.println("Unknown command '" + text + "'");
        System.out.println("Type '/help' for help.");
    }
}