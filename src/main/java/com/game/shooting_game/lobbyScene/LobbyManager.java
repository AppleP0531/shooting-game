package com.game.shooting_game.lobbyScene;

import java.awt.Graphics;
import java.util.Objects;

import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.domain.gacha.GachaService;
import com.game.shooting_game.domain.gacha.PieceService;
import com.game.shooting_game.domain.unlock.UnlockService;
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
import com.game.shooting_game.ui.UIBuilder;
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
    private JButton backpackBtn;
    private JButton nextShipBtn;
    private JButton prevShipBtn;
    private JButton selectShipBtn;
    private JButton unlockShipBtn;
    private JButton changeModeBtn;
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
        int iconSize = (int) (44 * adj0), iconGap = (int) (6 * adj0), edge = (int) (8 * adj0);
        int rowY = MAIN.getHeight() - (int) (5 * adj0) - iconSize;
        Vector2 upgradeBtnSize = new Vector2(iconSize, iconSize);
        String upgradeImagePath = "/static/img/UI/lobbyScene/upgradeIcon.png";
        upgradeBtn = UIBuilder.createImageBtn(component, upgradeImagePath, upgradeBtnSize, "UPGRADE");
        upgradeBtn.setBounds(edge + iconSize + iconGap, rowY, iconSize, iconSize);
        upgradeBtn.addActionListener(e -> SM.changeScene(SceneType.UPGRADE));
        upgradeBtn.setVisible(false); // 非表示
        this.add(upgradeBtn);

        // ショップボタン(ショップ画面に遷移。強化の右の四角いアイコン。解放前はグレーアウト)
        shopBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/shopIcon.png", upgradeBtnSize, "SHOP");
        shopBtn.setBounds(edge + (iconSize + iconGap) * 2, rowY, iconSize, iconSize);
        shopBtn.addActionListener(e -> SM.changeScene(SceneType.SHOP));
        shopBtn.setVisible(false); // 非表示
        this.add(shopBtn);
        UnlockService.guard(shopBtn, Feature.SHOP);

        // ガチャボタン(ガチャ画面に遷移。ショップの右)
        gachaBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/gachaIcon.png", upgradeBtnSize, "GACHA");
        gachaBtn.setBounds(edge + (iconSize + iconGap) * 3, rowY, iconSize, iconSize);
        gachaBtn.addActionListener(e -> SM.changeScene(SceneType.GACHA));
        gachaBtn.setVisible(false); // 非表示
        this.add(gachaBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                36 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/SettingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        settingBtn.setBounds(
                MAIN.getWidth() - 10 - (int) settingBtn.getWidth(),
                (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) settingBtn.getWidth(), (int) settingBtn.getHeight());
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

        // セレクトボタン(船を選択)
        Vector2 selectShipBtnSize = new Vector2(
                180 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        String selectShipImagePath = "/static/img/UI/lobbyScene/selectShip.png";
        selectShipBtn = UIBuilder.createImageBtn(component, selectShipImagePath, selectShipBtnSize,
                "SELLECT");
        selectShipBtn.setBounds(
                MAIN.getWidth() / 2 - (int) selectShipBtn.getWidth() / 2,
                MAIN.getHeight() - (int) selectShipBtn.getHeight() / 2
                        - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                (int) selectShipBtn.getWidth(), (int) selectShipBtn.getHeight());
        selectShipBtn.addActionListener(e -> {
            DataManager.getUserData().setSelectedShip(ships[viewingShipInd]);
            updateBtnStates();
        });
        selectShipBtn.setVisible(false); // 非表示
        this.add(selectShipBtn);

        // アンロックボタン(船を開放)
        Vector2 unlockShipBtnSize = new Vector2(
                180 * ShootingGameMain.getScreenSizeAdj(),
                36 * ShootingGameMain.getScreenSizeAdj());
        String unlockShipImagePath = "/static/img/UI/lobbyScene/unlockShip.png";
        unlockShipBtn = UIBuilder.createImageBtn(component, unlockShipImagePath, unlockShipBtnSize,
                "UNLOCK");
        unlockShipBtn.setBounds(
                MAIN.getWidth() / 2 - (int) unlockShipBtn.getWidth() / 2,
                MAIN.getHeight() - (int) unlockShipBtn.getHeight() / 2
                        - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                (int) unlockShipBtn.getWidth(), (int) unlockShipBtn.getHeight());
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

        // モード変更ボタン(モードを変更)
        Vector2 changeModeBtnSize = new Vector2(
                40 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String changeModeImagePath = "/static/img/UI/lobbyScene/modeIcon.png";
        changeModeBtn = UIBuilder.createImageBtn(component, changeModeImagePath, changeModeBtnSize,
                "CHANGE_MODE");
        changeModeBtn.setBounds(
                MAIN.getWidth() - (int) changeModeBtn.getWidth() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                MAIN.getHeight() - (int) changeModeBtn.getHeight()
                        - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                (int) changeModeBtn.getWidth(), (int) changeModeBtn.getHeight());
        changeModeBtn.addActionListener(e -> {
            SHIP_M.getPlayer().changeMode();
        });
        changeModeBtn.setVisible(false); // 非表示
        this.add(changeModeBtn);

        // 鞄ボタン(バックパック画面に遷移)
        Vector2 backpackBtnSize = new Vector2(iconSize, iconSize);
        String backpackImagePath = "/static/img/UI/lobbyScene/bagIcon.png";
        backpackBtn = UIBuilder.createImageBtn(component, backpackImagePath, backpackBtnSize, "Bag");
        backpackBtn.setBounds(edge, rowY, iconSize, iconSize);
        backpackBtn.addActionListener(e -> SM.changeScene(SceneType.BAG));
        backpackBtn.setVisible(false); // 非表示
        this.add(backpackBtn);

        // ステージ選択ボタン(ステージ選択画面に遷移)
        int stageX = edge + (iconSize + iconGap) * 4;
        Vector2 selectStageBtnSize = new Vector2(
                MAIN.getWidth() - stageX - edge,
                iconSize);
        String selectStageImagePath = "/static/img/UI/lobbyScene/selectStage.png";
        selectStageBtn = UIBuilder.createImageBtn(component, selectStageImagePath,
                selectStageBtnSize, "SELECT STAGE");
        selectStageBtn.setBounds(stageX, rowY, (int) selectStageBtnSize.getX(), iconSize);
        selectStageBtn
                .addActionListener(e -> SM.changeScene(SceneType.SELECTSTAGE));
        selectStageBtn.setVisible(false); // 非表示
        this.add(selectStageBtn);
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
        String text = "ピース " + PieceService.ownedText(ships[viewingShipInd]) + " / " + PieceService.UNLOCK_PIECES + " + "
                + String.format("%,d", cost) + " コイン";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.setColor(enough ? new java.awt.Color(255, 214, 80) : new java.awt.Color(255, 120, 120));
        g2.drawString(text, unlockShipBtn.getX() + (unlockShipBtn.getWidth() - fm.stringWidth(text)) / 2,
                unlockShipBtn.getY() - (int) (6 * adj));
        g2.dispose();
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
        nextShipBtn.setVisible(true);
        prevShipBtn.setVisible(true);
        backpackBtn.setVisible(true);
        selectStageBtn.setVisible(true);
        changeModeBtn.setVisible(true);

        selectShipBtn.setVisible(false);
        upgradeBtn.setVisible(true); // 強化ボタンは、未開放の船でもグレーアウトで見せる
        unlockShipBtn.setVisible(false);

        // プレビュー中の船を所有しているかどうか
        boolean isOwned = DataManager.getUserData().getShips().containsKey(ships[viewingShipInd]);
        // プレビュー中の船が選択されているかどうか
        boolean isSelected = DataManager.getUserData().getSelectedShip().equals(ships[viewingShipInd]);
        // 開放しているかどうか
        if (isOwned) {
            // 未選択なら選択ボタンを表示
            if (!isSelected)
                selectShipBtn.setVisible(true);

            upgradeBtn.setEnabled(true);
        } else {
            unlockShipBtn.setVisible(true); // アンロックボタンを表示
            upgradeBtn.setEnabled(false); // 未開放の船は強化できない(隠さず、グレーアウトする)
        }
    }
}
