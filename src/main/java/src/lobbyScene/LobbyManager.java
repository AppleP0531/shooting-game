package src.lobbyScene;

import src.SceneManager;
import src.ShootingGameMain;
import src.data.DataManager;
import src.gameScene.bullet.BulletManager;
import src.gameScene.ship.ShipManager;
import src.gameScene.ship.ShipManager.ShipKinds;
import src.interfaces.Manager;
import src.lobbyScene.LobbySceneUI.HangarSceneUIData;
import src.utils.UIBuilder;
import src.utils.Vector2;
import src.SceneManager.SceneType;

import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JButton;

public final class LobbyManager extends JPanel implements Manager {
        private static final String component = "[LobbyManager]";
        private static LobbyManager instance;
        private final LobbySceneUI lobbyUI;

        private final ShootingGameMain MAIN;
        private final SceneManager SM;
        private final ShipManager SHIP_M;
        private final BulletManager BM;

        private JButton upgradeButton;
        private JButton settingButton;
        private JButton backpackButton;
        private JButton nextShipButton;
        private JButton previousShipButton;
        private JButton sellectShipButton;
        private JButton unlockShipButton;
        private JButton selectStageButton;

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
                Vector2 upgradeButtonSize = new Vector2(
                                150 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String upgradeImagePath = "/static/img/UI/lobbyScene/upgrade.png";
                upgradeButton = UIBuilder.createImageButton(component, upgradeImagePath, upgradeButtonSize, "UPGRADE");
                upgradeButton.setBounds(
                                MAIN.getWidth() / 2 - (int) upgradeButton.getWidth() / 2,
                                MAIN.getHeight() - (int) upgradeButton.getHeight()
                                                - (int) (50 * ShootingGameMain.getScreenSizeAdj()),
                                (int) upgradeButton.getWidth(), (int) upgradeButton.getHeight());
                upgradeButton.addActionListener(e -> SM.changeScene(SceneType.UPGRADE));
                this.add(upgradeButton);

                // 設定ボタン(設定画面に遷移)
                Vector2 settingButtonSize = new Vector2(
                                30 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String settingImagePath = "/static/img/UI/lobbyScene/settingImage.png";
                settingButton = UIBuilder.createImageButton(component, settingImagePath, settingButtonSize, "SETTING");
                settingButton.setBounds(
                                MAIN.getWidth() - 10 - (int) settingButton.getWidth(),
                                (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                                (int) settingButton.getWidth(), (int) settingButton.getHeight());
                settingButton.addActionListener(e -> SM.changeScene(SceneType.SETTING));
                this.add(settingButton);

                // ネクストシップボタン(次の船にシフト)
                Vector2 nextShipButtonSize = new Vector2(
                                30 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String nextShipImagePath = "/static/img/UI/lobbyScene/nextShip.png";
                nextShipButton = UIBuilder.createImageButton(component, nextShipImagePath, nextShipButtonSize, ">");
                nextShipButton.setBounds(
                                MAIN.getWidth() / 2
                                                + (int) (150 * ShootingGameMain.getScreenSizeAdj())
                                                - (int) nextShipButtonSize.getX() / 2,
                                MAIN.getHeight() / 2 - (int) nextShipButtonSize.getY() / 2,
                                (int) nextShipButton.getWidth(), (int) nextShipButton.getHeight());
                nextShipButton.addActionListener(e -> {
                        viewingShipInd = (viewingShipInd + 1) % ships.length;
                        BM.clearBullets();
                        updateButtonStates();
                });
                this.add(nextShipButton);

                // プレビオスボタン(前の船にシフト)
                Vector2 previousShipButtonSize = new Vector2(
                                30 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String previousShipImagePath = "/static/img/UI/lobbyScene/previousShip.png";
                previousShipButton = UIBuilder.createImageButton(component, previousShipImagePath,
                                previousShipButtonSize, "<");
                previousShipButton.setBounds(
                                MAIN.getWidth() / 2
                                                - (int) (150 * ShootingGameMain.getScreenSizeAdj())
                                                - (int) nextShipButtonSize.getX() / 2,
                                MAIN.getHeight() / 2 - (int) previousShipButtonSize.getY() / 2,
                                (int) previousShipButton.getWidth(), (int) previousShipButton.getHeight());
                previousShipButton.addActionListener(e -> {
                        viewingShipInd = (viewingShipInd - 1 + ships.length) % ships.length;
                        BM.clearBullets();
                        updateButtonStates();
                });
                this.add(previousShipButton);

                // セレクトボタン(船を選択)
                Vector2 sellectShipButtonSize = new Vector2(
                                150 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String sellectShipImagePath = "/static/img/UI/lobbyScene/sellectShip.png";
                sellectShipButton = UIBuilder.createImageButton(component, sellectShipImagePath, sellectShipButtonSize,
                                "SELLECT");
                sellectShipButton.setBounds(
                                MAIN.getWidth() / 2 - (int) sellectShipButton.getWidth() / 2,
                                MAIN.getHeight() - (int) sellectShipButton.getHeight() / 2
                                                - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                                (int) sellectShipButton.getWidth(), (int) sellectShipButton.getHeight());
                sellectShipButton.addActionListener(e -> {
                        DataManager.getUserData().setSelectedShip(ships[viewingShipInd]);
                        updateButtonStates();
                });
                this.add(sellectShipButton);

                // アンロックボタン(船を開放)
                Vector2 unlockShipButtonSize = new Vector2(
                                150 * ShootingGameMain.getScreenSizeAdj(),
                                30 * ShootingGameMain.getScreenSizeAdj());
                String unlockShipImagePath = "/static/img/UI/lobbyScene/unlockShip.png";
                unlockShipButton = UIBuilder.createImageButton(component, unlockShipImagePath, unlockShipButtonSize,
                                "UNLOCK");
                unlockShipButton.setBounds(
                                MAIN.getWidth() / 2 - (int) unlockShipButton.getWidth() / 2,
                                MAIN.getHeight() - (int) unlockShipButton.getHeight() / 2
                                                - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                                (int) unlockShipButton.getWidth(), (int) unlockShipButton.getHeight());
                unlockShipButton.addActionListener(e -> {
                        int targetShipCost = SHIP_M.getShipConfig().get(ShipKinds.PLAYER).get(ships[viewingShipInd])
                                        .getDatas()
                                        .getCost();
                        int ownedCoins = DataManager.getUserData().getResources().getCoins();
                        if (ownedCoins > targetShipCost) {
                                DataManager.getUserData().unlockShip(ships[viewingShipInd]);
                                ownedCoins -= targetShipCost;
                                DataManager.getUserData().getResources().setCoins(ownedCoins);
                                updateButtonStates();
                        }
                });
                this.add(unlockShipButton);

                // 鞄ボタン(バックパック画面に遷移)
                Vector2 backpackButtonSize = new Vector2(
                                200 * ShootingGameMain.getScreenSizeAdj(),
                                40 * ShootingGameMain.getScreenSizeAdj());
                String backpackImagePath = "/static/img/UI/lobbyScene/backpackImage.png";
                backpackButton = UIBuilder.createImageButton(component, backpackImagePath, backpackButtonSize, "Bag");
                backpackButton.setBounds(
                                MAIN.getWidth() / 2 - (int) backpackButton.getWidth(),
                                MAIN.getHeight() - (int) backpackButton.getHeight()
                                                - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                                (int) backpackButton.getWidth(), (int) backpackButton.getHeight());
                backpackButton.addActionListener(e -> SM.changeScene(SceneType.BAG));
                this.add(backpackButton);

                // ステージ選択ボタン(ステージ選択画面に遷移)
                Vector2 selectStageButtonSize = new Vector2(
                                200 * ShootingGameMain.getScreenSizeAdj(),
                                40 * ShootingGameMain.getScreenSizeAdj());
                String selectStageImagePath = "/static/img/UI/lobbyScene/selectStage.png";
                selectStageButton = UIBuilder.createImageButton(component, selectStageImagePath,
                                selectStageButtonSize, "SELECT STAGE");
                selectStageButton.setBounds(
                                MAIN.getWidth() / 2,
                                MAIN.getHeight() - (int) selectStageButton.getHeight()
                                                - (int) (5 * ShootingGameMain.getScreenSizeAdj()),
                                (int) selectStageButton.getWidth(), (int) selectStageButton.getHeight());
                selectStageButton
                                .addActionListener(e -> SM.changeScene(SceneType.SELECTSTAGE));
                this.add(selectStageButton);
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
                updateButtonStates();
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
         * 選択ボタン、アンロックボタンの表示非表示
         */
        public void updateButtonStates() {
                // プレビュー中の船を所有しているかどうか
                boolean isOwned = DataManager.getUserData().getShips().containsKey(ships[viewingShipInd]);
                // プレビュー中の船が選択されているかどうか
                boolean isSelected = DataManager.getUserData().getSelectedShip().equals(ships[viewingShipInd]);
                // 所有しているかつ未選択の場合セレクトボタンを表示
                sellectShipButton.setVisible(isOwned && !isSelected);
                // 未所持ならアンロックボタン表示
                unlockShipButton.setVisible(!isOwned);
        }
}
