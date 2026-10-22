package com.game.shooting_game.stageScene.normal;

import java.awt.Color;
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
import com.game.shooting_game.domain.star.StarRewardService;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.stageScene.StarRewardPanel;
import com.game.shooting_game.stageScene.normal.subPanels.StageBtnPanel;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.ButtonAnimator;
import com.game.shooting_game.ui.ModeButtonImage;
import com.game.shooting_game.ui.UIBuilder;
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
    private JButton starBtn;
    private StarRewardPanel starRewardPanel;

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
        // 右下の「戻る」ボタン(青い板。下の帯の中)
        double adj = ShootingGameMain.getScreenSizeAdj();
        int backW = (int) (96 * adj), backH = (int) (38 * adj);
        lobbyBtn = UIBuilder.createImageBtn(component,
                ModeButtonImage.create(backW, backH, "戻る", new Color(90, 190, 255), new Color(20, 80, 190),
                        new Color(190, 235, 255), (int) (17 * adj)),
                new Vector2(backW, backH), "LOBBY");
        UIBuilder.placeBtn(lobbyBtn, Anchor.BOTTOM_RIGHT, Anchor.BOTTOM_RIGHT, -8, -9);
        lobbyBtn.addActionListener(e -> SM.changeScene(SceneType.LOBBY));
        ButtonAnimator.attachPop(lobbyBtn);
        this.add(lobbyBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        UIBuilder.placeBtn(settingBtn, Anchor.TOP_RIGHT, Anchor.TOP_RIGHT, -8, 7);
        ButtonAnimator.attachPop(settingBtn);
        settingBtn.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        this.add(settingBtn);

        // ★報酬(★の数と次の目標を出し、押すと報酬パネルを開く)
        Vector2 starBtnSize = new Vector2(
                150 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        starBtn = UIBuilder.createImageBtn(component, (BufferedImage) null, starBtnSize, "★");
        // 右上の設定ボタンの左(左上のコイン表示と重ならないように)
        starBtn.setBounds(
                MAIN.getWidth() - 10 - (int) settingBtn.getWidth() - (int) (5 * ShootingGameMain.getScreenSizeAdj())
                        - (int) starBtn.getWidth(),
                (int) (3 * ShootingGameMain.getScreenSizeAdj()), (int) starBtn.getWidth(),
                (int) (38 * ShootingGameMain.getScreenSizeAdj()));
        starBtn.addActionListener(e -> starRewardPanel.showPanel());
        this.add(starBtn);

        starRewardPanel = new StarRewardPanel(this::refreshStarBtn);
        this.add(starRewardPanel);
        refreshStarBtn();

        stageBtnPanel.initComponents();
    }

    /** ★ボタンの表記を更新する(受け取れる報酬があれば "!" を付けて黄色にする) */
    private void refreshStarBtn() {
        if (starBtn == null)
            return;
        boolean claimable = StarRewardService.claimableCount() > 0;
        starBtn.setText("★ " + StarRewardService.totalStars() + "/" + StarRewardService.nextTarget()
                + (claimable ? " !" : ""));
        starBtn.setForeground(claimable ? java.awt.Color.YELLOW : java.awt.Color.WHITE);
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
            LogUtils.debug(component,
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
            LogUtils.debug(component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.fatal(component,
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
                    case "FILE_NOT_FOUND" -> LogUtils.warn(component,
                            "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                    // 画像読み込みエラーログ
                    case "BROKEN_FILE" -> LogUtils.error(component,
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
        refreshStarBtn();
        stageBtnPanel.setup();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        lobbyUI.draw(g, stageBtnPanel.getScrollOffset());
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