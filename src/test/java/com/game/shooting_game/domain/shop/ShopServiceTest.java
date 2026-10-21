package com.game.shooting_game.domain.shop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.stage.DifficultyService;

class ShopServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
    }

    @AfterEach
    void tearDown() {
        DataManager.setAdminMode(false);
    }

    private void coins(int n) {
        DataManager.getUserData().getResources().setCoins(n);
    }

    private int coins() {
        return DataManager.getUserData().getResources().getCoins();
    }

    @Test
    @DisplayName("設定から3商品が読める")
    void items() {
        assertThat(ShopService.items()).extracting(ShopService.ShopItem::kind)
                .containsExactly(ItemKinds.BOMB, ItemKinds.FREEZE, ItemKinds.SKIP_TICKET);
        assertThat(ShopService.find(ItemKinds.BOMB).price()).isEqualTo(800000);
        assertThat(ShopService.find(ItemKinds.FREEZE).price()).isEqualTo(1000000);
        assertThat(ShopService.find(ItemKinds.SKIP_TICKET).price()).isEqualTo(1500000);
    }

    @Test
    @DisplayName("コインが足りれば購入でき、コインが減って所持数が増える")
    void buySuccess() {
        coins(5_000_000);
        assertThat(ShopService.buy(ItemKinds.BOMB)).isTrue();
        assertThat(coins()).isEqualTo(4_200_000);
        assertThat(ShopService.owned(ItemKinds.BOMB)).isEqualTo(1);
    }

    @Test
    @DisplayName("コインが足りなければ買えず、何も変わらない")
    void notEnoughCoins() {
        coins(799_999);
        assertThat(ShopService.canBuy(ItemKinds.BOMB)).isFalse();
        assertThat(ShopService.buy(ItemKinds.BOMB)).isFalse();
        assertThat(coins()).isEqualTo(799_999);
        assertThat(ShopService.owned(ItemKinds.BOMB)).isZero();
    }

    @Test
    @DisplayName("所持上限を超えては買えない")
    void maxOwned() {
        coins(100_000_000);
        for (int i = 0; i < 10; i++)
            assertThat(ShopService.buy(ItemKinds.BOMB)).isTrue();
        assertThat(ShopService.buy(ItemKinds.BOMB)).isFalse();
        assertThat(ShopService.owned(ItemKinds.BOMB)).isEqualTo(10);
        assertThat(coins()).isEqualTo(100_000_000 - 8_000_000);
    }

    @Test
    @DisplayName("consume は所持数を1つ減らし、0なら false")
    void consume() {
        assertThat(ShopService.consume(ItemKinds.FREEZE)).isFalse();
        coins(1_000_000);
        ShopService.buy(ItemKinds.FREEZE);
        assertThat(ShopService.consume(ItemKinds.FREEZE)).isTrue();
        assertThat(ShopService.owned(ItemKinds.FREEZE)).isZero();
        assertThat(ShopService.consume(ItemKinds.FREEZE)).isFalse();
    }

    @Test
    @DisplayName("管理者モードではコインが足りなくても買え、コインは減らない")
    void adminMode() {
        DataManager.setAdminMode(true);
        coins(0);
        assertThat(ShopService.buy(ItemKinds.BOMB)).isTrue();
        assertThat(coins()).isZero();
        assertThat(ShopService.owned(ItemKinds.BOMB)).isEqualTo(1);
    }

    @Test
    @DisplayName("保存キーは inv:KIND で、★の数には影響しない")
    void keyFormat() {
        int before = DifficultyService.totalStars();
        coins(5_000_000);
        ShopService.buy(ItemKinds.BOMB);
        ShopService.buy(ItemKinds.FREEZE);
        assertThat(ShopService.key(ItemKinds.BOMB)).isEqualTo("inv:BOMB");
        assertThat(DataManager.getUserData().getProgress().getHighScores())
                .containsEntry("inv:BOMB", 1).containsEntry("inv:FREEZE", 1);
        assertThat(DifficultyService.totalStars()).isEqualTo(before);
    }

    @Test
    @DisplayName("純粋関数 canBuy")
    void pureCanBuy() {
        assertThat(ShopService.canBuy(100, 100, 0, 10, false)).isTrue();
        assertThat(ShopService.canBuy(99, 100, 0, 10, false)).isFalse();
        assertThat(ShopService.canBuy(0, 100, 0, 10, true)).isTrue();
        assertThat(ShopService.canBuy(1000, 100, 10, 10, true)).isFalse();
    }
}
