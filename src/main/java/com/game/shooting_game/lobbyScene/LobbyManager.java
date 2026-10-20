package com.game.shooting_game.lobbyScene;

import com.game.shooting_game.domain.cost.CostService;
import java.awt.Graphics;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.lobbyScene.LobbySceneUI.HangarSceneUIData;
import com.game.shooting_game.utils.LogUtils;
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
    private JButton settingBtn;
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
        Vector2 upgradeBtnSize = new Vector2(
                150 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String upgradeImagePath = "/static/img/UI/lobbyScene/upgrade.png";
        upgradeBtn = UIBuilder.createImageBtn(component, upgradeImagePath, upgradeBtnSize, "UPGRADE");
        upgradeBtn.setBounds(
                MAIN.getWidth() / 2 - (int) upgradeBtn.getWidth() / 2,
                MAIN.getHeight() - (int) upgradeBtn.getHeight()
                        - (int) (50 * ShootingGameMain.getScreenSizeAdj()),
                (int) upgradeBtn.getWidth(), (int) upgradeBtn.getHeight());
        upgradeBtn.addActionListener(e -> SM.changeScene(SceneType.UPGRADE));
        upgradeBtn.setVisible(false); // 非表示
        this.add(upgradeBtn);

        // 設定ボタン(設定画面に遷移)
        Vector2 settingBtnSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String settingImagePath = "/static/img/UI/lobbyScene/settingImage.png";
        settingBtn = UIBuilder.createImageBtn(component, settingImagePath, settingBtnSize, "SETTING");
        settingBtn.setBounds(
                MAIN.getWidth() - 10 - (int) settingBtn.getWidth(),
                (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                (int) settingBtn.getWidth(), (int) settingBtn.getHeight());
        settingBtn.addActionListener(e -> SM.changeScene(SceneType.SETTING));
        settingBtn.setVisible(false); // 非表示
        this.add(settingBtn);

        // ネクストシップボタン(次の船にシフト)
        Vector2 nextShipBtnSize = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String nextShipImagePath = "/static/img/UI/lobbyScene/nextShip.png";
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
                30 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String prevShipImagePath = "/static/img/UI/lobbyScene/prevShip.png";
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
                150 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
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
                150 * ShootingGameMain.getScreenSizeAdj(),
                30 * ShootingGameMain.getScreenSizeAdj());
        String unlockShipImagePath = "/static/img/UI/lobbyScene/unlockShip.png";
        unlockShipBtn = UIBuilder.createImageBtn(component, unlockShipImagePath, unlockShipBtnSize,
                "UNLOCK");
        unlockShipBtn.setBounds(
                MAIN.getWidth() / 2 - (int) unlockShipBtn.getWidth() / 2,
                MAIN.getHeight() - (int) unlockShipBtn.getHeight() / 2
                        - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                (int) unlockShipBtn.getWidth(), (int) unlockShipBtn.getHeight());
        unlockShipBtn.addActionListener(e -> {
            int targetShipCost = SHIP_M.getShipConfig().get(ShipKinds.PLAYER).get(ships[viewingShipInd])
                    .getDatas()
                    .getCost();
            int ownedCoins = CostService.coins();
            if (ownedCoins >= targetShipCost) {
                DataManager.getUserData().unlockShip(ships[viewingShipInd]);
                // 管理者モードはコインを減らさない
                if (!DataManager.isAdminMode())
                    DataManager.getUserData().getResources().setCoins(ownedCoins - targetShipCost);
                updateBtnStates();
            } else {
                LogUtils.systemLog("[INFO]", component, "There is no enough money");
                lobbyUI.showMessage("コインが足りません（必要: " + targetShipCost + " / 所持: " + ownedCoins + "）");
            }
        });
        unlockShipBtn.setVisible(false); // 非表示
        this.add(unlockShipBtn);

        // モード変更ボタン(モードを変更)
        Vector2 changeModeBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                20 * ShootingGameMain.getScreenSizeAdj());
        String changeModeImagePath = "/static/img/UI/lobbyScene/changeMode.png";
        changeModeBtn = UIBuilder.createImageBtn(component, changeModeImagePath, changeModeBtnSize,
                "CHANGE_MODE");
        changeModeBtn.setBounds(
                MAIN.getWidth() - (int) changeModeBtn.getWidth() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                MAIN.getHeight() - (int) changeModeBtn.getHeight() / 2
                        - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                (int) changeModeBtn.getWidth(), (int) changeModeBtn.getHeight());
        changeModeBtn.addActionListener(e -> {
            SHIP_M.getPlayer().changeMode();
        });
        changeModeBtn.setVisible(false); // 非表示
        this.add(changeModeBtn);

        // 鞄ボタン(バックパック画面に遷移)
        Vector2 backpackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String backpackImagePath = "/static/img/UI/lobbyScene/backpackImage.png";
        backpackBtn = UIBuilder.createImageBtn(component, backpackImagePath, backpackBtnSize, "Bag");
        backpackBtn.setBounds(
                MAIN.getWidth() / 2 - (int) backpackBtn.getWidth(),
                MAIN.getHeight() - (int) backpackBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) backpackBtn.getWidth(), (int) backpackBtn.getHeight());
        backpackBtn.addActionListener(e -> SM.changeScene(SceneType.BAG));
        backpackBtn.setVisible(false); // 非表示
        this.add(backpackBtn);

        // ステージ選択ボタン(ステージ選択画面に遷移)
        Vector2 selectStageBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                40 * ShootingGameMain.getScreenSizeAdj());
        String selectStageImagePath = "/static/img/UI/lobbyScene/selectStage.png";
        selectStageBtn = UIBuilder.createImageBtn(component, selectStageImagePath,
                selectStageBtnSize, "SELECT STAGE");
        selectStageBtn.setBounds(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - (int) selectStageBtn.getHeight()
                        - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                (int) selectStageBtn.getWidth(), (int) selectStageBtn.getHeight());
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
        nextShipBtn.setVisible(true);
        prevShipBtn.setVisible(true);
        backpackBtn.setVisible(true);
        selectStageBtn.setVisible(true);
        changeModeBtn.setVisible(true);

        selectShipBtn.setVisible(false);
        upgradeBtn.setVisible(false);
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

            upgradeBtn.setVisible(true); // 強化ボタンを表示
        } else {
            unlockShipBtn.setVisible(true); // アンロックボタンを表示
        }
    }
}
