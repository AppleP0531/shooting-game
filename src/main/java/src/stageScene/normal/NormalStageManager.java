package src.stageScene.normal;

import src.SceneManager;
import src.SceneManager.SceneType;
import src.ShootingGameMain;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;
import src.interfaces.Manager;
import src.stageScene.normal.subPanels.StageButtonPanel;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.JButton;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ノーマルステージシーン管理クラス
 */
public final class NormalStageManager extends JPanel implements Manager {
    private static final String component = "[NormalStageManager]";
    private static NormalStageManager instance;
    private final NormalStageSceneUI lobbyUI;
    @Getter
    private final StageButtonPanel stageButtonPanel;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;

    // ----- データクラスの定義 -----
    /**
     * ステージボタンの情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class StageButtonData {
        private int id;
        private Vector2 pos;
    }

    private JButton lobbyButton;
    private JButton settingButton;

    private List<StageButtonData> stageButtonConfigList; // ボタンデータ
    @Getter
    private Map<Integer, StageButtonData> stageButtonConfigMap; // ボタンデータマップ
    @Getter
    private Map<Integer, BufferedImage> stageButtonImageCache; // ボタン画像データ
    private static final TypeReference<List<StageButtonData>> TYPE_REF = new TypeReference<>() {
    };

    private NormalStageManager() {
        NormalStageManager.instance = this;
        lobbyUI = new NormalStageSceneUI();
        stageButtonPanel = new StageButtonPanel();

        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(stageButtonPanel);

        // ロビーにボタン(ロビー画面に遷移)
        Vector2 lobbyButtonSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/stageScene/normal/lobbyImage.png";
        lobbyButton = UIBuilder.createImageButton(component, lobbyImagePath, lobbyButtonSize, "LOBBY");
        lobbyButton.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyButton.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyButton.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyButton.getWidth(), (int) lobbyButton.getHeight());
        lobbyButton.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        this.add(lobbyButton);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingButtonSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingButton = UIBuilder.createImageButton(component, settingImagePath, settingButtonSize, "SETTING");
        settingButton.setBounds(
                MAIN.getWidth() - 10 - (int) settingButton.getWidth(),
                10,
                (int) settingButton.getWidth(), (int) settingButton.getHeight());
        settingButton.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        this.add(settingButton);

        stageButtonPanel.initComponents();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static NormalStageManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, NormalStageManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/StageButtonConfig.json";
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            stageButtonConfigList = mapper.readValue(is, TYPE_REF);

            // Mapに変換
            stageButtonConfigMap = stageButtonConfigList.stream()
                    .collect(Collectors.toMap(StageButtonData::getId, data -> data));

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
    private void loadImages() {
        stageButtonImageCache = new HashMap<>();

        for (StageButtonData data : stageButtonConfigMap.values()) {
            String imagePath = "/static/img/UI/lobbyScene/stage" + data.getId() + ".png";
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
            stageButtonImageCache.put(data.getId(), image);
        }

    }

    @Override
    public void setup() {
        reload();
        stageButtonPanel.setup();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        lobbyUI.draw(g);
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
        loadImages();
        stageButtonPanel.reload();
    }
}