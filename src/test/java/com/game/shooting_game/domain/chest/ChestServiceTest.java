package com.game.shooting_game.domain.chest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.chest.ChestService.Loot;
import com.game.shooting_game.domain.chest.ChestService.Opened;
import com.game.shooting_game.domain.chest.ChestService.Rank;
import com.game.shooting_game.domain.chest.ChestService.Table;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.stage.Difficulty;

class ChestServiceTest {
    private static final int NOW = 1_000_000;

    @BeforeEach
    void setUp() {
        DataManager.create("tester");
        DataManager.setAdminMode(false);
        ChestService.reload();
    }

    private Map<String, Integer> store() {
        return DataManager.getUserData().getProgress().getHighScores();
    }

    private void giveTickets(int n) {
        store().put(ShopService.key(ItemKinds.SKIP_TICKET), n);
    }

    @Test
    @DisplayName("設定: 3ランクすべてに表があり、時間は正、コインは最小<=最大、確率は0〜1")
    void config() {
        for (Rank rank : Rank.values()) {
            Table t = ChestService.table(rank);
            assertThat(t).isNotNull();
            assertThat(t.openSeconds()).isPositive();
            assertThat(ChestService.openSeconds(rank)).isEqualTo(t.openSeconds());
            assertThat(t.coinMin()).isLessThanOrEqualTo(t.coinMax());
            assertThat(t.resources()).isNotNull();
            t.resources().forEach(e -> {
                assertThat(e.chance()).isBetween(0.0, 1.0);
                assertThat(e.min()).isLessThanOrEqualTo(e.max());
            });
            assertThat(t.modules()).isNotNull(); // 空でも可
            t.modules().forEach(m -> assertThat(m.chance()).isBetween(0.0, 1.0));
        }
        // ランクが上がるほど、開くのに時間がかかる
        assertThat(ChestService.openSeconds(Rank.COPPER)).isLessThan(ChestService.openSeconds(Rank.SILVER));
        assertThat(ChestService.openSeconds(Rank.SILVER)).isLessThan(ChestService.openSeconds(Rank.GOLD));
    }

    @Test
    @DisplayName("鍵: 増やせる。0以下は無視")
    void keys() {
        assertThat(ChestService.keyCount(Rank.COPPER)).isZero();
        ChestService.addKey(Rank.COPPER, 2);
        ChestService.addKey(Rank.COPPER, 0);
        ChestService.addKey(Rank.COPPER, -5);
        assertThat(ChestService.keyCount(Rank.COPPER)).isEqualTo(2);
        assertThat(ChestService.keyCount(Rank.SILVER)).isZero();
        ChestService.addKey(Rank.SILVER, 1);
        assertThat(ChestService.keyCount(Rank.SILVER)).isEqualTo(1);
        assertThat(ChestService.keyCount(Rank.COPPER)).isEqualTo(2);
    }

    @Test
    @DisplayName("open: 鍵を1つ減らしてスロットを埋める")
    void openUsesKey() {
        ChestService.addKey(Rank.SILVER, 2);
        assertThat(ChestService.usedSlots()).isZero();
        assertThat(ChestService.freeSlot()).isZero();

        int slot = ChestService.open(Rank.SILVER, NOW);
        assertThat(slot).isZero();
        assertThat(ChestService.keyCount(Rank.SILVER)).isEqualTo(1);
        assertThat(ChestService.slotRank(slot)).isEqualTo(Rank.SILVER);
        assertThat(ChestService.usedSlots()).isEqualTo(1);
        assertThat(ChestService.freeSlot()).isEqualTo(1);
        assertThat(ChestService.open(Rank.SILVER, NOW)).isEqualTo(1);
        assertThat(ChestService.keyCount(Rank.SILVER)).isZero();
    }

    @Test
    @DisplayName("open: 鍵がない・10個埋まっているときは -1 で鍵は減らない")
    void openFails() {
        assertThat(ChestService.open(Rank.GOLD, NOW)).isEqualTo(-1);
        assertThat(ChestService.usedSlots()).isZero();

        ChestService.addKey(Rank.COPPER, ChestService.MAX_SLOTS + 1);
        for (int i = 0; i < ChestService.MAX_SLOTS; i++)
            assertThat(ChestService.open(Rank.COPPER, NOW)).isEqualTo(i);
        assertThat(ChestService.usedSlots()).isEqualTo(ChestService.MAX_SLOTS);
        assertThat(ChestService.freeSlot()).isEqualTo(-1);
        assertThat(ChestService.keyCount(Rank.COPPER)).isEqualTo(1);

        assertThat(ChestService.open(Rank.COPPER, NOW)).isEqualTo(-1);
        assertThat(ChestService.keyCount(Rank.COPPER)).isEqualTo(1);
        assertThat(ChestService.usedSlots()).isEqualTo(ChestService.MAX_SLOTS);
    }

    @Test
    @DisplayName("残り時間・受け取れるか・受け取れる数が時刻に応じて変わる")
    void timing() {
        ChestService.addKey(Rank.COPPER, 1);
        int secs = ChestService.openSeconds(Rank.COPPER);
        int slot = ChestService.open(Rank.COPPER, NOW);

        assertThat(ChestService.remaining(slot, NOW)).isEqualTo(secs);
        assertThat(ChestService.remaining(slot, NOW + 100)).isEqualTo(secs - 100);
        assertThat(ChestService.isReady(slot, NOW + secs - 1)).isFalse();
        assertThat(ChestService.readyCount(NOW + secs - 1)).isZero();
        assertThat(ChestService.isReady(slot, NOW + secs)).isTrue();
        assertThat(ChestService.remaining(slot, NOW + secs + 50)).isZero();
        assertThat(ChestService.readyCount(NOW + secs)).isEqualTo(1);

        // 空のスロットは 0 秒・受け取れない
        assertThat(ChestService.remaining(5, NOW)).isZero();
        assertThat(ChestService.isReady(5, NOW + 999999)).isFalse();
        assertThat(ChestService.slotRank(5)).isNull();
        assertThat(ChestService.slotRank(-1)).isNull();
        assertThat(ChestService.slotRank(ChestService.MAX_SLOTS)).isNull();
    }

    @Test
    @DisplayName("skip: 券を1枚使って即開封になる")
    void skipWithTicket() {
        ChestService.addKey(Rank.GOLD, 1);
        int slot = ChestService.open(Rank.GOLD, NOW);
        giveTickets(2);

        assertThat(ChestService.isReady(slot, NOW)).isFalse();
        assertThat(ChestService.skip(slot, NOW)).isTrue();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(1);
        assertThat(ChestService.isReady(slot, NOW)).isTrue();
        assertThat(ChestService.remaining(slot, NOW)).isZero();

        // すでに開き終わっているものには使わない(券は減らない)
        assertThat(ChestService.skip(slot, NOW)).isFalse();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(1);
        // 空のスロットにも使えない
        assertThat(ChestService.skip(7, NOW)).isFalse();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(1);
    }

    @Test
    @DisplayName("skip: 券がないときは false で、何も変わらない")
    void skipWithoutTicket() {
        ChestService.addKey(Rank.COPPER, 1);
        int slot = ChestService.open(Rank.COPPER, NOW);
        int before = ChestService.remaining(slot, NOW);

        assertThat(ChestService.skip(slot, NOW)).isFalse();
        assertThat(ChestService.remaining(slot, NOW)).isEqualTo(before);
        assertThat(ChestService.isReady(slot, NOW)).isFalse();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isZero();
    }

    @Test
    @DisplayName("roll: 乱数を固定すると再現でき、コインは範囲内")
    void rollReproducible() {
        for (Rank rank : Rank.values()) {
            Table t = ChestService.table(rank);
            for (long seed = 0; seed < 50; seed++) {
                Loot a = ChestService.roll(rank, new Random(seed));
                Loot b = ChestService.roll(rank, new Random(seed));
                assertThat(a).isEqualTo(b);
                assertThat(a.coins()).isBetween(t.coinMin(), t.coinMax());
                assertThat(a.resources().values()).allMatch(n -> n > 0);
            }
        }
    }

    @Test
    @DisplayName("roll: 上のランクほどコインが多い(設定の関係)")
    void rollBetterRank() {
        Table copper = ChestService.table(Rank.COPPER);
        Table silver = ChestService.table(Rank.SILVER);
        Table gold = ChestService.table(Rank.GOLD);
        assertThat(silver.coinMin()).isGreaterThanOrEqualTo(copper.coinMax());
        assertThat(gold.coinMin()).isGreaterThanOrEqualTo(silver.coinMax());
        for (long seed = 0; seed < 30; seed++) {
            assertThat(ChestService.roll(Rank.GOLD, new Random(seed)).coins())
                    .isGreaterThan(ChestService.roll(Rank.COPPER, new Random(seed)).coins());
        }
    }

    @Test
    @DisplayName("claim: 開き終わっていなければ null で、スロットも埋まったまま")
    void claimNotReady() {
        ChestService.addKey(Rank.COPPER, 1);
        int slot = ChestService.open(Rank.COPPER, NOW);
        long coins = DataManager.getUserData().getResources().getCoins();

        assertThat(ChestService.claim(slot, NOW, new Random(1))).isNull();
        assertThat(ChestService.claim(3, NOW, new Random(1))).isNull(); // 空のスロット
        assertThat(ChestService.slotRank(slot)).isEqualTo(Rank.COPPER);
        assertThat(DataManager.getUserData().getResources().getCoins()).isEqualTo(coins);
    }

    @Test
    @DisplayName("claim: 開き終わっていれば、コイン・素材が入り、スロットが空き、二重には受け取れない")
    void claimReady() {
        ChestService.addKey(Rank.COPPER, 1);
        int slot = ChestService.open(Rank.COPPER, NOW);
        int ready = NOW + ChestService.openSeconds(Rank.COPPER);
        var res = DataManager.getUserData().getResources();
        int coinsBefore = res.getCoins();
        Loot expected = ChestService.roll(Rank.COPPER, new Random(42));

        Opened o = ChestService.claim(slot, ready, new Random(42));
        assertThat(o).isNotNull();
        assertThat(o.loot()).isEqualTo(expected);
        assertThat(res.getCoins()).isEqualTo(coinsBefore + expected.coins());
        expected.resources().forEach((k, v) -> assertThat(res.getEquipments().get(k)).isGreaterThanOrEqualTo(v));
        assertThat(ChestService.slotRank(slot)).isNull();
        assertThat(ChestService.usedSlots()).isZero();
        assertThat(ChestService.freeSlot()).isEqualTo(slot);
        assertThat(o.modules()).hasSameSizeAs(expected.moduleRarities());

        assertThat(ChestService.claim(slot, ready, new Random(42))).isNull();
        assertThat(res.getCoins()).isEqualTo(coinsBefore + expected.coins());
    }

    @Test
    @DisplayName("claim: モジュールを含む中身では、モジュールが作られて所持に入る")
    void claimWithModules() {
        long seed = -1;
        for (long s = 0; s < 1000; s++) {
            if (!ChestService.roll(Rank.GOLD, new Random(s)).moduleRarities().isEmpty()) {
                seed = s;
                break;
            }
        }
        assertThat(seed).isGreaterThanOrEqualTo(0);
        ChestService.addKey(Rank.GOLD, 1);
        int slot = ChestService.open(Rank.GOLD, NOW);
        int ready = NOW + ChestService.openSeconds(Rank.GOLD);
        int ownedBefore = DataManager.getUserData().getModules().getOwned().size();

        Opened o = ChestService.claim(slot, ready, new Random(seed));
        assertThat(o).isNotNull();
        assertThat(o.modules()).isNotEmpty().doesNotContainNull();
        assertThat(o.modules()).hasSameSizeAs(o.loot().moduleRarities());
        assertThat(DataManager.getUserData().getModules().getOwned().size())
                .isEqualTo(ownedBefore + o.modules().size());
    }

    @Test
    @DisplayName("鍵のランクは難易度に応じる(EASY・NORMAL=銅、HARD=銀、NIGHTMARE=金)")
    void keyRankByDifficulty() {
        assertThat(ChestService.firstClearKey(Difficulty.EASY)).isEqualTo(Rank.COPPER);
        assertThat(ChestService.firstClearKey(Difficulty.NORMAL)).isEqualTo(Rank.COPPER);
        assertThat(ChestService.firstClearKey(Difficulty.HARD)).isEqualTo(Rank.SILVER);
        assertThat(ChestService.firstClearKey(Difficulty.NIGHTMARE)).isEqualTo(Rank.GOLD);
        assertThat(ChestService.bossKey(Difficulty.EASY)).isEqualTo(Rank.COPPER);
        assertThat(ChestService.bossKey(Difficulty.NORMAL)).isEqualTo(Rank.COPPER);
        assertThat(ChestService.bossKey(Difficulty.HARD)).isEqualTo(Rank.SILVER);
        assertThat(ChestService.bossKey(Difficulty.NIGHTMARE)).isEqualTo(Rank.GOLD);
    }

    @Test
    @DisplayName("formatTime: 1時間以上は h:mm:ss、それ未満は m:ss")
    void formatTime() {
        assertThat(ChestService.formatTime(0)).isEqualTo("0:00");
        assertThat(ChestService.formatTime(5)).isEqualTo("0:05");
        assertThat(ChestService.formatTime(600)).isEqualTo("10:00");
        assertThat(ChestService.formatTime(3599)).isEqualTo("59:59");
        assertThat(ChestService.formatTime(3600)).isEqualTo("1:00:00");
        assertThat(ChestService.formatTime(14400 + 61)).isEqualTo("4:01:01");
    }
}
