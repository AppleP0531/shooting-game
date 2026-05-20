package com.game.shooting_game.upgradeScene;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.upgradeScene.subPanels.SkillTreeButtonPanel;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class UpgradeManager extends JPanel implements Manager {
    private static final String component = "[UpgradeManager]";
    private static UpgradeManager instance;
    private final UpgradeSceneUI upgradeUI;
    @Getter
    private final SkillTreeButtonPanel skillTreeButtonPanel;

    private final ShootingGameMain MAIN;

    // ----- データクラスの定義 -----
    /**
     * スキルツリーボタンの情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class SkillTreeData {
        private String name;
        private String rootNode;
        private int dist;
        private int pos;
        private String detail;
    }

    private JButton lobbyButton;

    @Getter
    private List<SkillTreeData> skillTreeConfigMap; // ボタンデータ
    @Getter
    private Map<String, BufferedImage> skillTreeImageCache; // ボタン画像データ
    private static final TypeReference<List<SkillTreeData>> TYPE_REF = new TypeReference<>() {
    };

    private UpgradeManager() {
        UpgradeManager.instance = this;
        upgradeUI = new UpgradeSceneUI();
        skillTreeButtonPanel = new SkillTreeButtonPanel();

        MAIN = ShootingGameMain.getInstance();

        reloadConfigs();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(skillTreeButtonPanel);

        // ロビーボタン(ロビー画面に戻るボタン)
        Vector2 lobbyButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/upgradeScene/lobbyImage.png";
        lobbyButton = UIBuilder.createImageButton(component, lobbyImagePath, lobbyButtonSize, "HANGER");
        lobbyButton.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyButton.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyButton.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyButton.getWidth(), (int) lobbyButton.getHeight());
        lobbyButton.addActionListener(e -> {
            skillTreeButtonPanel.removeHandler();
            SceneManager.getInstance().changeScene(SceneType.LOBBY);
        });
        this.add(lobbyButton);

        skillTreeButtonPanel.initComponents();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static UpgradeManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, UpgradeManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/SkillTreeConfig.json";
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to laod configuration form " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            skillTreeConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET);
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    private void loadSkillTreeButtonImages() {
        skillTreeImageCache = new HashMap<>();

        for (SkillTreeData data : skillTreeConfigMap) {
            String imagePath = "/static/img/UI/upgradeScene/" + data.getName() + ".png";
            BufferedImage image = null;
            try {
                image = UIBuilder.loadImage(imagePath);
            } catch (IOException e) {
                switch (e.getMessage()) {
                    // 画像ファイル未発見ログ
                    case "FILE_NOT_FOUND" -> LogUtils.systemLog(
                            "[WARN]",
                            component,
                            "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                    // 画像読み込みエラーログ
                    case "BROKEN_FILE" -> LogUtils.systemLog(
                            "[ERROR]",
                            component,
                            "Failed to load image: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                    // その他エラーログ
                    default -> System.out.println(e);
                }
            }
            skillTreeImageCache.put(data.getName(), image);
        }
    }

    @Override
    public void setup() {
        skillTreeButtonPanel.setup();
        skillTreeButtonPanel.addHandler();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        upgradeUI.draw(g);
        skillTreeButtonPanel.draw(g);
    }

    /**
     * 再読み込み
     */
    public void reload() {
        reloadConfigs();
        reloadScreen();
    }

    /**
     * リソースの再読み込み
     */
    private void reloadConfigs() {
        loadConfig();
        loadSkillTreeButtonImages();
    }

    /**
     * 画面状態の再読み込み
     */
    public void reloadScreen() {
        skillTreeButtonPanel.reload();
    }
}