package com.game.shooting_game.lobbyScene;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;

import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.domain.gacha.GachaService;
import com.game.shooting_game.domain.gacha.PieceService;
import com.game.shooting_game.domain.trial.TrialService;
import com.game.shooting_game.domain.unlock.UnlockService;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.domain.unlock.UnlockService.Feature;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.lobbyScene.LobbySceneUI.HangarSceneUIData;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.ButtonAnimator;
import com.game.shooting_game.ui.ModeButtonImage;
import com.game.shooting_game.ui.ShipPieceIcon;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.domain.armor.ArmorService;
import com.game.shooting_game.upgradeScene.subPanels.ArmorPanel;
import com.game.shooting_game.upgradeScene.subPanels.ConvertPanel;
import com.game.shooting_game.upgradeScene.subPanels.ModulePanel;
import com.game.shooting_game.upgradeScene.subPanels.SubShipPanel;
import com.game.shooting_game.upgradeScene.subPanels.UnitPanel;
import com.game.shooting_game.utils.Vector2;

public final class LobbyManager extends JPanel implements Manager {
    private static final String component = "[LobbyManager]";
    private static LobbyManager instance;
    private final LobbySceneUI lobbyUI;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final ShipManager SHIP_M;
    private final BulletManager BM;

    private JButton upgradeBtn;
    private JButton shopBtn;
    private JButton gachaBtn;
    private JButton settingBtn;
    private JButton dexBtn;
    private JButton achievementBtn;
    private JButton chestBtn;
    private JButton idleBtn;
    private JButton backpackBtn;
    private JButton nextShipBtn;
    private JButton prevShipBtn;
    private JButton unlockShipBtn;
    private JButton trialShipBtn;
    private JButton changeModeBtn;
    private JButton infinityBtn;
    private JButton moduleBtn;
    private JButton convertBtn;
    private final ModulePanel modulePanel = new ModulePanel();
    private final ConvertPanel convertPanel = new ConvertPanel();
    private final UnitPanel unitPanel = new UnitPanel();
    private final SubShipPanel subShipPanel = new SubShipPanel();
    private final ArmorPanel defensePanel = new ArmorPanel(ArmorService.Type.DEFENSE);
    private final ArmorPanel firepowerPanel = new ArmorPanel(ArmorService.Type.FIREPOWER);
    private Rectangle[] slotRects;
    private JButton[] slotBtns;
    private JButton selectStageBtn;

    private int viewingShipInd = -1;
    private final String[] ships = {
            "Ship1",
            "Ship2",
            "Ship3",
            "Ship4",
            "Ship5",
    };

    private LobbyManager() {
        LobbyManager.instance = this;
        lobbyUI = new LobbySceneUI();

        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        BM = BulletManager.getInstance();
    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        // 強化ボタン(強化画面に遷移)
        // 下の列は、左から「鞄」「強化」(四角いアイコン)と、残りを使う「ステージ選択」
        double adj0 = ShootingGameMain.getScreenSizeAdj();
        // 下は2段: 上に小さな四角いアイコン4つ(幅を6等分した中央に置く)、下に大きなモードボタン2つ
        int iconSize = (int) (40 * adj0), edge = (int) (8 * adj0);
        int bigH = (int) (46 * adj0);
        int bigY = MAIN.getHeight() - edge - bigH;
        int rowY = bigY - (int) (5 * adj0) - iconSize;
        int cellW = MAIN.getWidth() / 6; // 6つ: 鞄・強化・モジュール・変換・ショップ・ガチャ
        Vector2 upgradeBtnSize = new Vector2(iconSize, iconSize);
        String upgradeImagePath = "/static/img/UI/lobbyScene/upgradeIcon.png";
        upgradeBtn = UIBuilder.createImageBtn(component, upgradeImagePath, upgradeBtnSize, "UPGRADE");
        upgradeBtn.setBounds(cellW + (cellW - iconSize) / 2, rowY, iconSize, iconSize);
        upgradeBtn.addActionListener(e -> SM.changeScene(SceneType.UPGRADE));
        upgradeBtn.setVisible(false); // 非表示
        this.add(upgradeBtn);

        // ショップボタン(ショップ画面に遷移。強化の右の四角いアイコン。解放前はグレーアウト)
        shopBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/shopIcon.png", upgradeBtnSize, "SHOP");
        shopBtn.setBounds(cellW * 4 + (cellW - iconSize) / 2, rowY, iconSize, iconSize);
        shopBtn.addActionListener(e -> SM.changeScene(SceneType.SHOP));
        shopBtn.setVisible(false); // 非表示
        this.add(shopBtn);
        UnlockService.guard(shopBtn, Feature.SHOP);

        // モジュールボタン(強化画面から移した。強化の右)と、素材変換ボタン(その右)。四角い板に文字
        moduleBtn = UIBuilder.createImageBtn(component,
                ModeButtonImage.create(iconSize, iconSize, "モジュール", new Color(190, 130, 255),
                        new Color(90, 40, 170), new Color(230, 200, 255), (int) (8 * adj0)),
                new Vector2(iconSize, iconSize), "MODULE");
        moduleBtn.setBounds(cellW * 2 + (cellW - iconSize) / 2, rowY, iconSize, iconSize);
        moduleBtn.addActionListener(e -> modulePanel.showPanel());
        moduleBtn.setVisible(false); // 非表示
        this.add(moduleBtn);
        UnlockService.guard(moduleBtn, Feature.MODULE);

        convertBtn = UIBuilder.createImageBtn(component,
                ModeButtonImage.create(iconSize, iconSize, "変換", new Color(110, 220, 160),
                        new Color(20, 130, 90), new Color(200, 255, 225), (int) (12 * adj0)),
                new Vector2(iconSize, iconSize), "CONVERT");
        convertBtn.setBounds(cellW * 3 + (cellW - iconSize) / 2, rowY, iconSize, iconSize);
        convertBtn.addActionListener(e -> convertPanel.showPanel());
        convertBtn.setVisible(false); // 非表示
        this.add(convertBtn);

        // ガチャボタン(ガチャ画面に遷移。ショップの右)
        gachaBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/gachaIcon.png", upgradeBtnSize, "GACHA");
        gachaBtn.setBounds(cellW * 5 + (cellW - iconSize) / 2, rowY, iconSize, iconSize);
        gachaBtn.addActionListener(e -> SM.changeScene(SceneType.GACHA));
        gachaBtn.setVisible(false); // 非表示
        this.add(gachaBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                36 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        UIBuilder.placeBtn(settingBtn, Anchor.TOP_RIGHT, Anchor.TOP_RIGHT, -8, 4);
        settingBtn.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        settingBtn.setVisible(false); // 非表示
        this.add(settingBtn);

        // 図鑑ボタン(敵図鑑に遷移。設定ボタンの下)
        Vector2 dexBtnSize = new Vector2(
                36 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        dexBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/dexIcon.png", dexBtnSize, "図鑑");
        dexBtn.setBounds(
                MAIN.getWidth() - 10 - (int) dexBtn.getWidth(),
                settingBtn.getY() + (int) settingBtn.getHeight() + (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) dexBtn.getWidth(), (int) dexBtn.getHeight());
        dexBtn.addActionListener(e -> SM.changeScene(SceneType.ENCYCLOPEDIA));
        dexBtn.setVisible(false); // 非表示
        this.add(dexBtn);

        // 実績ボタン(実績の画面に遷移。図鑑ボタンの下)
        achievementBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/achievementIcon.png", dexBtnSize,
                "実績");
        achievementBtn.setBounds(
                MAIN.getWidth() - 10 - (int) achievementBtn.getWidth(),
                dexBtn.getY() + (int) dexBtn.getHeight() + (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) achievementBtn.getWidth(), (int) achievementBtn.getHeight());
        achievementBtn.addActionListener(e -> SM.changeScene(SceneType.ACHIEVEMENT));
        achievementBtn.setVisible(false); // 非表示
        this.add(achievementBtn);

        // 宝箱ボタン(宝箱の画面に遷移。実績ボタンの下)
        chestBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/chestIcon.png", dexBtnSize, "宝箱");
        chestBtn.setBounds(
                MAIN.getWidth() - 10 - (int) chestBtn.getWidth(),
                achievementBtn.getY() + (int) achievementBtn.getHeight() + (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) chestBtn.getWidth(), (int) chestBtn.getHeight());
        chestBtn.addActionListener(e -> SM.changeScene(SceneType.CHEST));
        chestBtn.setVisible(false); // 非表示
        this.add(chestBtn);

        // 放置ボタン(放置の画面に遷移。宝箱ボタンの下)
        idleBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/idleIcon.png", dexBtnSize, "放置");
        idleBtn.setBounds(
                MAIN.getWidth() - 10 - (int) idleBtn.getWidth(),
                chestBtn.getY() + (int) chestBtn.getHeight() + (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) idleBtn.getWidth(), (int) idleBtn.getHeight());
        idleBtn.addActionListener(e -> SM.changeScene(SceneType.IDLE));
        idleBtn.setVisible(false); // 非表示
        this.add(idleBtn);

        // ネクストシップボタン(次の船にシフト)
        Vector2 nextShipBtnSize = new Vector2(
                40 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String nextShipImagePath = "/static/img/UI/forwardArrow.png";
        nextShipBtn = UIBuilder.createImageBtn(component, nextShipImagePath, nextShipBtnSize, ">");
        nextShipBtn.setBounds(
                MAIN.getWidth() / 2
                        + (int) (150 * ShootingGameMain.getScreenSizeAdj())
                        - (int) nextShipBtnSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) nextShipBtnSize.getY() / 2,
                (int) nextShipBtn.getWidth(), (int) nextShipBtn.getHeight());
        nextShipBtn.addActionListener(e -> {
            viewingShipInd = (viewingShipInd + 1) % ships.length;
            BM.clearBullets();
            updateBtnStates();
        });
        nextShipBtn.setVisible(false); // 非表示
        this.add(nextShipBtn);

        // プレビオスボタン(前の船にシフト)
        Vector2 prevShipBtnSize = new Vector2(
                40 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String prevShipImagePath = "/static/img/UI/backArrow.png";
        prevShipBtn = UIBuilder.createImageBtn(component, prevShipImagePath,
                prevShipBtnSize, "<");
        prevShipBtn.setBounds(
                MAIN.getWidth() / 2
                        - (int) (150 * ShootingGameMain.getScreenSizeAdj())
                        - (int) nextShipBtnSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) prevShipBtnSize.getY() / 2,
                (int) prevShipBtn.getWidth(), (int) prevShipBtn.getHeight());
        prevShipBtn.addActionListener(e -> {
            viewingShipInd = (viewingShipInd - 1 + ships.length) % ships.length;
            BM.clearBullets();
            updateBtnStates();
        });
        prevShipBtn.setVisible(false); // 非表示
        this.add(prevShipBtn);

        // アンロックボタン(船を開放)
        Vector2 unlockShipBtnSize = new Vector2(
                180 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        String unlockShipImagePath = "/static/img/UI/lobbyScene/unlockShip.png";
        unlockShipBtn = UIBuilder.createImageBtn(component, unlockShipImagePath, unlockShipBtnSize,
                "UNLOCK");
        UIBuilder.placeBtn(unlockShipBtn, Anchor.BOTTOM_CENTER, Anchor.CENTER, 0, -197);
        unlockShipBtn.addActionListener(e -> {
            String id = ships[viewingShipInd];
            if (GachaService.unlock(id)) {
                updateBtnStates();
                return;
            }
            // 足りないものを知らせる(ピースはガチャで集める)
            int have = PieceService.owned(id);
            int cost = GachaService.shipCost(id);
            if (have < PieceService.UNLOCK_PIECES)
                lobbyUI.showMessage("ピースが足りません(必要: " + PieceService.UNLOCK_PIECES + " / 所持: " + have + "。ガチャで集めます)");
            else
                lobbyUI.showMessage("コインが足りません(必要: " + String.format("%,d", cost) + " / 所持: "
                        + String.format("%,d", CostService.coins()) + ")");
        });
        unlockShipBtn.setVisible(false); // 非表示
        this.add(unlockShipBtn);

        // お試しボタン(持っていない船を、トレーニングステージで試す。アンロックボタンの下)
        trialShipBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/trialShip.png", unlockShipBtnSize,
                "TRIAL");
        UIBuilder.placeBtn(trialShipBtn, Anchor.BOTTOM_CENTER, Anchor.CENTER, 0, -153);
        trialShipBtn.addActionListener(e -> {
            if (TrialService.begin(ships[viewingShipInd])) {
                StageManager.getInstance().getStage().setStageId(TrialService.TRIAL_STAGE_ID);
                StageManager.getInstance().getStage().setDifficulty(Difficulty.NORMAL);
                SM.changeScene(SceneType.GAME);
            }
        });
        trialShipBtn.setVisible(false); // 非表示
        this.add(trialShipBtn);

        // モード変更ボタン(モードを変更)
        Vector2 changeModeBtnSize = new Vector2(
                40 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String changeModeImagePath = "/static/img/UI/lobbyScene/modeIcon.png";
        changeModeBtn = UIBuilder.createImageBtn(component, changeModeImagePath, changeModeBtnSize,
                "CHANGE_MODE");
        UIBuilder.placeBtn(changeModeBtn, Anchor.BOTTOM_RIGHT, Anchor.BOTTOM_RIGHT, -10, -179);
        changeModeBtn.addActionListener(e -> {
            SHIP_M.getPlayer().changeMode();
        });
        changeModeBtn.setVisible(false); // 非表示
        this.add(changeModeBtn);

        // 鞄ボタン(バックパック画面に遷移)
        Vector2 backpackBtnSize = new Vector2(iconSize, iconSize);
        String backpackImagePath = "/static/img/UI/lobbyScene/bagIcon.png";
        backpackBtn = UIBuilder.createImageBtn(component, backpackImagePath, backpackBtnSize, "Bag");
        backpackBtn.setBounds((cellW - iconSize) / 2, rowY, iconSize, iconSize);
        backpackBtn.addActionListener(e -> SM.changeScene(SceneType.BAG));
        backpackBtn.setVisible(false); // 非表示
        this.add(backpackBtn);

        // 大きなモードボタン(左: 無限モード=橙、右: ステージ=黄。ステージ選択画面に遷移)
        int bigGap = (int) (8 * adj0);
        int bigW = (MAIN.getWidth() - edge * 2 - bigGap) / 2;
        int fontPx = (int) (20 * adj0);
        infinityBtn = UIBuilder.createImageBtn(component,
                ModeButtonImage.create(bigW, bigH, "無限モード", new Color(255, 170, 60), new Color(220, 70, 20),
                        new Color(255, 220, 150), fontPx),
                new Vector2(bigW, bigH), "INFINITY");
        infinityBtn.setBounds(edge, bigY, bigW, bigH);
        infinityBtn.addActionListener(e -> SM.changeScene(SceneType.INFINITYSTAGE));
        infinityBtn.setVisible(false); // 非表示
        this.add(infinityBtn);
        UnlockService.guard(infinityBtn, Feature.INFINITY);

        selectStageBtn = UIBuilder.createImageBtn(component,
                ModeButtonImage.create(bigW, bigH, "ステージ", new Color(255, 235, 90), new Color(235, 150, 20),
                        new Color(255, 250, 190), fontPx),
                new Vector2(bigW, bigH), "SELECT STAGE");
        selectStageBtn.setBounds(edge + bigW + bigGap, bigY, bigW, bigH);
        selectStageBtn
                .addActionListener(e -> SM.changeScene(SceneType.SELECTSTAGE));
        selectStageBtn.setVisible(false); // 非表示
        this.add(selectStageBtn);

        // 装備スロット(帯の上に4枚のカード。押すと、強化画面と同じパネルが開く)
        int cardGap = (int) (6 * adj0);
        int cardW = (int) (69 * adj0);
        int cardH = (int) (63 * adj0);
        int cardX0 = (MAIN.getWidth() - (cardW * 4 + cardGap * 3)) / 2; // 中央寄せ
        int cardY = MAIN.getHeight() - (int) ((104 + 6) * adj0) - cardH;
        LobbySceneUI.Slot[] slots = LobbySceneUI.Slot.values();
        slotRects = new Rectangle[slots.length];
        slotBtns = new JButton[slots.length];
        for (LobbySceneUI.Slot s : slots) {
            JButton b = new JButton();
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            slotRects[s.ordinal()] = new Rectangle(cardX0 + (cardW + cardGap) * s.ordinal(), cardY, cardW, cardH);
            b.setBounds(slotRects[s.ordinal()]);
            slotBtns[s.ordinal()] = b;
            this.add(b);
        }
        slotBtns[LobbySceneUI.Slot.UNIT.ordinal()].addActionListener(e -> unitPanel.showPanel());
        slotBtns[LobbySceneUI.Slot.SUBSHIP.ordinal()].addActionListener(e -> subShipPanel.showPanel());
        slotBtns[LobbySceneUI.Slot.DEFENSE.ordinal()].addActionListener(e -> defensePanel.showPanel());
        slotBtns[LobbySceneUI.Slot.FIREPOWER.ordinal()].addActionListener(e -> firepowerPanel.showPanel());
        UnlockService.guard(slotBtns[LobbySceneUI.Slot.SUBSHIP.ordinal()], Feature.SUBSHIP);
        this.add(unitPanel);
        this.add(subShipPanel);
        this.add(modulePanel);
        this.add(convertPanel);
        this.add(defensePanel);
        this.add(firepowerPanel);
        unitPanel.initComponents();
        subShipPanel.initComponents();
        modulePanel.initComponents();
        convertPanel.initComponents();
        defensePanel.initComponents();
        firepowerPanel.initComponents();

        // 押す・乗せると拡大縮小する(下の列・右上・船の切り替え・選択/解放・色の切り替え)
        for (JButton b : new JButton[] { upgradeBtn, shopBtn, gachaBtn, settingBtn, dexBtn, achievementBtn, chestBtn, idleBtn, nextShipBtn,
                prevShipBtn, unlockShipBtn, trialShipBtn, changeModeBtn, backpackBtn, infinityBtn, selectStageBtn,
                moduleBtn, convertBtn })
            ButtonAnimator.attachPop(b);
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static LobbyManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, LobbyManager::new);
    }

    @Override
    public void setup() {
        UnlockService.refreshAll();
        SwingUtilities.invokeLater(UnlockService::announceNewlyUnlocked);
        if (viewingShipInd == -1) {
            viewingShipInd = 0;
            setDefaultViewingShip(DataManager.getUserData().getSelectedShip());
        }

        BM.clearBullets();
        updateBtnStates();
        lobbyUI.setup();
    }

    @Override
    public void update(double dt) {
        lobbyUI.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        HangarSceneUIData uiData = new HangarSceneUIData(
                ships,
                viewingShipInd);
        lobbyUI.draw(g, uiData);
        drawSlots(g);
        drawModeIndicator(g);
        drawUnlockCost(g);
    }

    /**
     * 未開放の船を見ているとき、解放ボタンの上に必要なコインを出す(足りなければ赤)
     */
    private void drawUnlockCost(Graphics g) {
        if (unlockShipBtn == null || !unlockShipBtn.isVisible() || viewingShipInd < 0)
            return;
        var def = SHIP_M.getShipConfig().get(ShipKinds.PLAYER).get(ships[viewingShipInd]);
        if (def == null || def.getDatas() == null)
            return;
        int cost = def.getDatas().getCost();
        boolean enough = DataManager.isAdminMode() || CostService.coins() >= cost;
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, Math.max(10, (int) (13 * adj))));
        int havePieces = PieceService.owned(ships[viewingShipInd]);
        enough = enough && havePieces >= PieceService.UNLOCK_PIECES;
        String text = PieceService.ownedText(ships[viewingShipInd]) + " / " + PieceService.UNLOCK_PIECES + " + "
                + String.format("%,d", cost) + " コイン";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int icon = (int) (26 * adj), gap = (int) (6 * adj);
        int left = unlockShipBtn.getX() + (unlockShipBtn.getWidth() - (icon + gap + fm.stringWidth(text))) / 2;
        int baseY = unlockShipBtn.getY() - (int) (6 * adj);
        // 弾やビームが後ろを通っても読めるよう、暗い板を敷く
        int padX = (int) (8 * adj), padY = (int) (4 * adj);
        int plateH = icon + padY * 2;
        g2.setColor(new java.awt.Color(0, 8, 24, 215));
        g2.fillRoundRect(left - padX, baseY - icon + (int) (4 * adj) - padY,
                icon + gap + fm.stringWidth(text) + padX * 2, plateH, 12, 12);
        ShipPieceIcon.draw(g2, ships[viewingShipInd], left, baseY - icon + (int) (4 * adj), icon);
        g2.setColor(enough ? new java.awt.Color(255, 214, 80) : new java.awt.Color(255, 120, 120));
        g2.drawString(text, left + icon + gap, baseY);
        g2.dispose();
    }

    /**
     * 装備スロットのカードの描画(マウスが乗っているか、未解放かをボタンから読む)
     */
    private void drawSlots(Graphics g) {
        if (slotBtns == null)
            return;
        boolean[] hovered = new boolean[slotBtns.length];
        boolean[] locked = new boolean[slotBtns.length];
        for (int i = 0; i < slotBtns.length; i++)
            hovered[i] = slotBtns[i].isEnabled() && slotBtns[i].getModel().isRollover();
        locked[LobbySceneUI.Slot.SUBSHIP.ordinal()] = !UnlockService.isUnlocked(Feature.SUBSHIP);
        lobbyUI.drawSlots(g, slotRects, hovered, locked);
    }

    /**
     * いまの色(青 / 赤)を、色の切り替えボタンのまわりに出す(ボタンの枠の光と、下の文字)
     */
    private void drawModeIndicator(Graphics g) {
        if (changeModeBtn == null || !changeModeBtn.isVisible() || SHIP_M.getPlayer() == null)
            return;
        boolean red = SHIP_M.getPlayer().getMode() == com.game.shooting_game.gameScene.GameManager.ColorMode.RED;
        java.awt.Color c = red ? new java.awt.Color(255, 90, 100) : new java.awt.Color(90, 150, 255);
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = (int) (5 * adj);
        int x = changeModeBtn.getX() - pad, y = changeModeBtn.getY() - pad;
        int w = changeModeBtn.getWidth() + pad * 2, h = changeModeBtn.getHeight() + pad * 2;
        // 外側へ薄くなる光 + はっきりした枠
        for (int i = 3; i >= 1; i--) {
            g2.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g2.setStroke(new java.awt.BasicStroke((float) (i * 3 * adj)));
            g2.drawRoundRect(x, y, w, h, (int) (16 * adj), (int) (16 * adj));
        }
        g2.setColor(c);
        g2.setStroke(new java.awt.BasicStroke((float) (2.5 * adj)));
        g2.drawRoundRect(x, y, w, h, (int) (16 * adj), (int) (16 * adj));
        // 下に、いまの色の名前
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, Math.max(10, (int) (13 * adj))));
        String text = red ? "RED" : "BLUE";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, changeModeBtn.getX() + (changeModeBtn.getWidth() - fm.stringWidth(text)) / 2,
                y + h + fm.getAscent() + (int) (2 * adj));
        g2.dispose();
    }

    /**
     * デフォルトの参照船を設定(ログアウト時に使用していた船を参照)
     */
    public void setDefaultViewingShip(String shipName) {
        // ログアウト時に使用していた船をデフォルト参照にする
        for (int i = 0; i < ships.length; i++) {
            if (shipName.equals(ships[i])) {
                viewingShipInd = i;
                break;
            }
        }
    }

    /**
     * ボタンの表示非表示
     */
    public void updateBtnStates() {
        settingBtn.setVisible(true);
        shopBtn.setVisible(true);
        gachaBtn.setVisible(true);
        dexBtn.setVisible(true);
        achievementBtn.setVisible(true);
        chestBtn.setVisible(true);
        idleBtn.setVisible(true);
        nextShipBtn.setVisible(true);
        prevShipBtn.setVisible(true);
        backpackBtn.setVisible(true);
        infinityBtn.setVisible(true);
        moduleBtn.setVisible(true);
        convertBtn.setVisible(true);
        selectStageBtn.setVisible(true);
        changeModeBtn.setVisible(true);

        upgradeBtn.setVisible(true); // 強化ボタンは、未開放の船でもグレーアウトで見せる
        unlockShipBtn.setVisible(false);
        trialShipBtn.setVisible(false);

        // プレビュー中の船を所有しているかどうか
        boolean isOwned = DataManager.getUserData().getShips().containsKey(ships[viewingShipInd]);
        // 開放している船は、見ている船がそのまま選択中になる(選択ボタンはない)
        if (isOwned) {
            DataManager.getUserData().setSelectedShip(ships[viewingShipInd]);

            upgradeBtn.setEnabled(true);
        } else {
            unlockShipBtn.setVisible(true); // アンロックボタンを表示
            trialShipBtn.setVisible(TrialService.canTry(ships[viewingShipInd])); // お試しボタン
            upgradeBtn.setEnabled(false); // 未開放の船は強化できない(隠さず、グレーアウトする)
        }
    }
}
