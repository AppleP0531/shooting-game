package com.game.shooting_game.shopScene;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.domain.shop.ShopService.ShopItem;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.shopScene.ShopSceneUI.CardData;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ShopManager extends JPanel implements Manager {
    private static final String component = "[ShopManager]";
    private static ShopManager instance;

    private final ShopSceneUI shopUI = new ShopSceneUI();
    private final List<JButton> buyBtns = new ArrayList<>();
    private final List<CardData> cards = new ArrayList<>();
    private JButton goBackBtn;
    private String message = "";

    private ShopManager() {

    }

    public void initComponents() {
        setLayout(null);
        setOpaque(false);
        double adj = ShootingGameMain.getScreenSizeAdj();
        int width = ShootingGameMain.getInstance().getWidth();
        int height = ShootingGameMain.getInstance().getHeight();

        // 商品ごとの「買う」ボタン(カードの右下)
        buyBtns.clear();
        List<ShopItem> items = ShopService.items();
        for (int i = 0; i < items.size(); i++) {
            ItemKinds kind = items.get(i).kind();
            JButton btn = UIBuilder.createImageBtn(component, (BufferedImage) null,
                    new Vector2(84 * adj, 32 * adj), "買う");
            Rectangle r = ShopSceneUI.cardRect(i, width);
            btn.setBounds(r.x + r.width - btn.getWidth() - (int) (12 * adj),
                    r.y + r.height - btn.getHeight() - (int) (10 * adj), btn.getWidth(), btn.getHeight());
            btn.setFocusable(false);
            btn.addActionListener(e -> buy(kind));
            buyBtns.add(btn);
            this.add(btn);
        }

        // 戻るボタン(一つ前の画面に遷移)
        goBackBtn = UIBuilder.createBackBtn(component, () -> SceneManager.getInstance().changeToPrevScene());
        this.add(goBackBtn);
    }

    private void buy(ItemKinds kind) {
        ShopItem it = ShopService.find(kind);
        if (ShopService.buy(kind)) {
            message = (it == null ? "" : it.name() + "を") + "購入しました";
        } else if (it != null && ShopService.owned(kind) >= it.maxOwned()) {
            message = "これ以上持てません";
        } else {
            message = "コインが足りません";
        }
        refresh();
    }

    /** ステージのアイテム絵がない商品の絵(なければnull) */
    private BufferedImage shopIcon(ItemKinds kind) {
        if (kind != ItemKinds.SKIP_TICKET)
            return null;
        try {
            return UIBuilder.loadImage("/static/img/UI/shopScene/skipTicket.png");
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    /** 所持数・ボタンの有効/無効を作り直す */
    private void refresh() {
        cards.clear();
        Map<ItemKinds, BufferedImage[]> images = ItemManager.getInstance().getItemImage();
        List<ShopItem> items = ShopService.items();
        for (int i = 0; i < items.size(); i++) {
            ShopItem it = items.get(i);
            BufferedImage[] frames = images == null ? null : images.get(it.kind());
            BufferedImage icon = frames != null && frames.length > 0 ? frames[0] : null;
            if (icon == null) // ステージには出ない商品(研究スキップ券など)は、専用の絵
                icon = shopIcon(it.kind());
            cards.add(new CardData(it, ShopService.owned(it.kind()), icon));
            if (i < buyBtns.size())
                buyBtns.get(i).setEnabled(DataManager.getUserData() != null && ShopService.canBuy(it.kind()));
        }
    }

    /**
     * インスタンスの取得
     *
     * @return インスタンス
     */
    public static ShopManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, ShopManager::new);
    }

    @Override
    public void setup() {
        message = "";
        refresh();
    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {
        shopUI.draw(g, cards, message);
    }
}
