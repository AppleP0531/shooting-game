package com.game.shooting_game.upgradeScene;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

import com.game.shooting_game.domain.unlock.UnlockService;
import com.game.shooting_game.domain.unlock.UnlockService.Feature;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.upgradeScene.subPanels.SkillTreeBtnPanel;
import com.game.shooting_game.upgradeScene.subPanels.ConvertPanel;
import com.game.shooting_game.upgradeScene.subPanels.SubShipPanel;
import com.game.shooting_game.upgradeScene.subPanels.UnitPanel;
import com.game.shooting_game.upgradeScene.subPanels.ModulePanel;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class UpgradeManager extends JPanel implements Manager {
    private static final String component = "[UpgradeManager]";
    private static UpgradeManager instance;
    private final UpgradeSceneUI upgradeUI;
    @Getter
    private final SkillTreeBtnPanel skillTreeBtnPanel;
    private final SubShipPanel subShipPanel;
    private final ConvertPanel convertPanel;
    private final UnitPanel unitPanel;
    private final ModulePanel modulePanel;

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
        private String title; // ボタンに表示する短い名前
        private String effect; // 効果の種類(SkillServiceが集計する)
        private double perLevel; // 1レベルあたりの効果量
        private int maxLevel = 1; // 最大レベル
        private String unit = ""; // 表示単位("%"は100倍して表示)
        private boolean reduce = false; // 効果が「減る」ものか(表示を「-」にする。値そのものは正の数のまま)
        private int reqLevel = 1; // 解放に必要な親ノードのレベル
        private int coin; // 初回強化に必要なコイン
        private double coinGrowth = 1.0; // レベルごとのコイン倍率
        private int researchSec = 30; // 初回強化の研究時間(秒)
        private double researchGrowth = 1.5; // レベルごとの研究時間の倍率
        // レベルごとの必要素材 [レベル0→1, 1→2, ...]。素材名 → レア度 → 個数
        private List<Map<String, Map<String, Integer>>> matLevels = new ArrayList<>();
    }

    private JButton lobbyBtn;
    private JButton subShipBtn;
    private JButton convertBtn;
    private JButton unitBtn;
    private JButton moduleBtn;

    @Getter
    private List<SkillTreeData> skillTreeConfigMap; // ボタンデータ
    @Getter
    private Map<String, BufferedImage> skillTreeImageCache; // ボタン画像データ
    private static final TypeReference<List<SkillTreeData>> TYPE_REF = new TypeReference<>() {
    };

    /**
     * 子のパネルが重なるので、重なりを考慮して描く(前面のパネルの上に、背面のボタンが描かれてしまうのを防ぐ)
     */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    private UpgradeManager() {
        UpgradeManager.instance = this;
        upgradeUI = new UpgradeSceneUI();
        skillTreeBtnPanel = new SkillTreeBtnPanel();
        subShipPanel = new SubShipPanel();
        convertPanel = new ConvertPanel();
        unitPanel = new UnitPanel();
        modulePanel = new ModulePanel();

        MAIN = ShootingGameMain.getInstance();

        reloadConfigs();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        this.add(skillTreeBtnPanel);

        // ロビーボタン(ロビー画面に戻るボタン)
        Vector2 lobbyBtnSize = new Vector2(
                185 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String lobbyImagePath = "/static/img/UI/upgradeScene/lobbyImage.png";
        lobbyBtn = UIBuilder.createImageBtn(component, lobbyImagePath, lobbyBtnSize, "HANGER");
        lobbyBtn.setBounds(
                MAIN.getWidth() / 2 - (int) lobbyBtn.getWidth() - (int) (4 * ShootingGameMain.getScreenSizeAdj()),
                MAIN.getHeight() - (int) lobbyBtn.getHeight() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) lobbyBtn.getWidth(), (int) lobbyBtn.getHeight());
        lobbyBtn.addActionListener(e -> {
            skillTreeBtnPanel.removeHandler();
            SceneManager.getInstance().changeScene(SceneType.LOBBY);
        });
        this.add(lobbyBtn);

        // サブ機工房ボタン(パーツの制作と装備)
        Vector2 subShipBtnSize = new Vector2(
                185 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String subShipImagePath = "/static/img/UI/upgradeScene/subShipImage.png";
        subShipBtn = UIBuilder.createImageBtn(component, subShipImagePath, subShipBtnSize, "SUB SHIP");
        subShipBtn.setBounds(
                MAIN.getWidth() / 2 + (int) (4 * ShootingGameMain.getScreenSizeAdj()),
                lobbyBtn.getY(),
                (int) subShipBtn.getWidth(), (int) subShipBtn.getHeight());
        subShipBtn.addActionListener(e -> subShipPanel.showPanel());
        this.add(subShipBtn);
        UnlockService.guard(subShipBtn, Feature.SUBSHIP);

        // 素材変換ボタン(5個 → 1つ上のレア度)。下のボタン2つの上に並べる
        Vector2 convertBtnSize = new Vector2(
                122 * ShootingGameMain.getScreenSizeAdj(),
                44 * ShootingGameMain.getScreenSizeAdj());
        String convertImagePath = "/static/img/UI/upgradeScene/convertImage.png";
        convertBtn = UIBuilder.createImageBtn(component, convertImagePath, convertBtnSize, "CONVERT");
        convertBtn.setBounds(
                MAIN.getWidth() / 2 - (int) (189 * ShootingGameMain.getScreenSizeAdj()),
                lobbyBtn.getY() - (int) convertBtn.getHeight() - (int) (6 * ShootingGameMain.getScreenSizeAdj()),
                (int) convertBtn.getWidth(), (int) convertBtn.getHeight());
        convertBtn.addActionListener(e -> convertPanel.showPanel());
        this.add(convertBtn);

        // 機体強化ボタン(機体のレベルと★)。変換ボタンの隣
        Vector2 unitBtnSize = new Vector2(convertBtnSize);
        unitBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/unitImage.png", unitBtnSize, "UNIT");
        unitBtn.setBounds(
                MAIN.getWidth() / 2 - (int) (61 * ShootingGameMain.getScreenSizeAdj()),
                convertBtn.getY(),
                (int) unitBtn.getWidth(), (int) unitBtn.getHeight());
        unitBtn.addActionListener(e -> unitPanel.showPanel());
        this.add(unitBtn);

        // モジュールボタン(モジュールの制作と装着)
        moduleBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/moduleImage.png",
                new Vector2(convertBtnSize), "MODULE");
        moduleBtn.setBounds(
                MAIN.getWidth() / 2 + (int) (67 * ShootingGameMain.getScreenSizeAdj()),
                convertBtn.getY(),
                (int) moduleBtn.getWidth(), (int) moduleBtn.getHeight());
        moduleBtn.addActionListener(e -> modulePanel.showPanel());
        this.add(moduleBtn);
        UnlockService.guard(moduleBtn, Feature.MODULE);

        this.add(subShipPanel);
        this.add(convertPanel);
        this.add(unitPanel);
        this.add(modulePanel);

        skillTreeBtnPanel.initComponents();
        subShipPanel.initComponents();
        convertPanel.initComponents();
        unitPanel.initComponents();
        modulePanel.initComponents();
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
    private void loadSkillTreeBtnImages() {
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
        UnlockService.refreshAll();
        skillTreeBtnPanel.setup();
        skillTreeBtnPanel.addHandler();
    }

    @Override
    public void update(double dt) {
        // 研究が終わったらボタンのレベル表示を更新する
        if (SkillService.tickResearch())
            reloadScreen();
    }

    @Override
    public void draw(Graphics g) {
        upgradeUI.draw(g);
        skillTreeBtnPanel.draw(g);
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
        loadSkillTreeBtnImages();
    }

    /**
     * 画面状態の再読み込み
     */
    public void reloadScreen() {
        skillTreeBtnPanel.reload();
    }
}