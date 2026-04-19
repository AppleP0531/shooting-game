package src.upgradeScene;

import src.SceneManager;
import src.SceneManager.SceneType;
import src.ShootingGameMain;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;
import src.interfaces.Manager;
import src.upgradeScene.subPanels.SkillTreeButtonPanel;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public static class SkillTreeButtonData {
        private String name;
        private String rootNode;
        private int dist;
        private int pos;
    }

    private JButton lobbyButton;

    @Getter
    private List<SkillTreeButtonData> skillTreeButtonConfigMap; // ボタンデータ
    @Getter
    private Map<String, BufferedImage> skillTreeButtonImageCache; // ボタン画像データ
    private static final TypeReference<List<SkillTreeButtonData>> TYPE_REF = new TypeReference<>() {
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
        final String CONFIG_PATH = "/static/configs/SkillTreeButtonConfig.json";
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
            skillTreeButtonConfigMap = mapper.readValue(is, TYPE_REF);

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
        skillTreeButtonImageCache = new HashMap<>();

        for (SkillTreeButtonData data : skillTreeButtonConfigMap) {
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
            skillTreeButtonImageCache.put(data.getName(), image);
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
    private void reloadScreen() {
        skillTreeButtonPanel.reload();
    }
}