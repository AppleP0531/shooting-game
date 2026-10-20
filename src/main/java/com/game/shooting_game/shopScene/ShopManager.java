package com.game.shooting_game.shopScene;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ShopManager extends JPanel implements Manager {
    private static final String component = "[ShopManager]";
    private static ShopManager instance;

    private JButton goBackBtn;

    private ShopManager() {

    }

    public void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        // 戻るボタン(一つ前の画面に遷移)
        Vector2 goBackBtnSize = new Vector2(
                200 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String goBackImagePath = "/static/img/UI/shopScene/goBackImage.png";
        goBackBtn = UIBuilder.createImageBtn(component, goBackImagePath, goBackBtnSize,
                "GO BACK");
        goBackBtn.setPreferredSize(
                new Dimension((int) goBackBtn.getWidth(), (int) goBackBtn.getHeight()));
        goBackBtn.addActionListener(e -> {
            SceneManager.getInstance().changeToPrevScene();
        });
        this.add(goBackBtn);
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

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {

    }
}
