package com.game.shooting_game.stageScene.normal;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import com.game.shooting_game.stageScene.normal.subPanels.StageBtnPanel;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class NormalStageManager extends JPanel implements Manager {
    private static final String component = "[NormalStageManager]";
    private static NormalStageManager instance;
    private final NormalStageSceneUI lobbyUI;
    @Getter
    private final StageBtnPanel stageBtnPanel;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;

    // ----- データクラスの定義 -----
    /**
     * ステージボタンの情報
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class StageBtnData {
        private int id;
        private Vector2 pos;
    }

    private JButton lobbyBtn;
    private JButton settingBtn;

    private List<StageBtnData> stageBtnConfigList; // ボタンデータ
    @Getter
    private Map<Integer, StageBtnData> stageBtnConfigMap; // ボタンデータマップ
    @Getter
    private Map<Integer, BufferedImage> stageBtnImageCache; // ボタン画像データ
    private static final TypeReference<List<StageBtnData>> TYPE_REF = new TypeReference<>() {
    };

    private NormalStageManager() {
        NormalStageManager.instance = this;
        lobbyUI = new NormalStageSceneUI();
        stageBtnPanel = new StageBtnPanel();

        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();

        reloadConfigs();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(stageBtnPanel);

        // ロビーにボタン(ロビー画面に遷移)
        Vector2 lobbyBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/stageScene/normal/lobbyImage.png";
        lobbyBtn = UIBuilder.createImageBtn(component, lobbyImagePath, lobbyBtnSize, "LOBBY");
        lobbyBtn.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyBtn.getWidth() / 2,
                MAIN.getHeight() - (int) lobbyBtn.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyBtn.getWidth(), (int) lobbyBtn.getHeight());
        lobbyBtn.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        this.add(lobbyBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        settingBtn.setBounds(
                MAIN.getWidth() - 10 - (int) settingBtn.getWidth(),
                10,
                (int) settingBtn.getWidth(), (int) settingBtn.getHeight());
        settingBtn.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        this.add(settingBtn);

        stageBtnPanel.initComponents();
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
        final String CONFIG_PATH = "/static/configs/StageBtnConfig.json";
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
            stageBtnConfigList = mapper.readValue(is, TYPE_REF);

            // Mapに変換
            stageBtnConfigMap = stageBtnConfigList.stream()
                    .collect(Collectors.toMap(StageBtnData::getId, data -> data));

            // ロード成功ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.error(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET + "\n"
                            + ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    private void loadImages() {
        stageBtnImageCache = new HashMap<>();

        for (StageBtnData data : stageBtnConfigMap.values()) {
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
            stageBtnImageCache.put(data.getId(), image);
        }

    }

    @Override
    public void setup() {
        stageBtnPanel.setup();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        lobbyUI.draw(g);
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
        loadImages();
    }

    /**
     * 画面状態の再読み込み
     */
    private void reloadScreen() {
        stageBtnPanel.reload();
    }
}