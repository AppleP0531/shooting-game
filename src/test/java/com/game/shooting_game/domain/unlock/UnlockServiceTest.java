package com.game.shooting_game.domain.unlock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.swing.JButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.unlock.UnlockService.Feature;

class UnlockServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.load("tester", "create"); // Exp.json の読み込みも行う
        UnlockService.reload();
    }

    /** そのプレイヤーレベルになるのに必要な経験値の総量を与える */
    private void setLevel(int level) {
        int exp = 0;
        List<Integer> data = DataManager.getExpData();
        for (int i = 0; i < level; i++)
            exp += data.get(i);
        DataManager.getUserData().getProgress().setExp(exp);
        assertThat(UnlockService.playerLevel()).isEqualTo(level);
    }

    @Test
    @DisplayName("isUnlocked はレベルが必要レベル以上のときだけ true")
    void isUnlockedPure() {
        assertThat(UnlockService.isUnlocked(4, 5)).isFalse();
        assertThat(UnlockService.isUnlocked(5, 5)).isTrue();
        assertThat(UnlockService.isUnlocked(6, 5)).isTrue();
    }

    @Test
    @DisplayName("設定どおりの解放レベル")
    void requiredLevels() {
        assertThat(UnlockService.requiredLevel(Feature.SHOP)).isEqualTo(5);
        assertThat(UnlockService.requiredLevel(Feature.MODULE)).isEqualTo(10);
        assertThat(UnlockService.requiredLevel(Feature.EXTRA_MODULE)).isEqualTo(10);
        assertThat(UnlockService.requiredLevel(Feature.SUBSHIP)).isEqualTo(15);
        assertThat(UnlockService.requiredLevel(Feature.INFINITY)).isEqualTo(20);
        assertThat(UnlockService.requiredLevel(Feature.EXTRA_BOSS)).isEqualTo(25);
        assertThat(UnlockService.requiredLevel(Feature.EXTRA_MATERIAL)).isEqualTo(25);
        assertThat(UnlockService.requiredLevel(Feature.EXTRA_SPECIAL)).isEqualTo(30);
    }

    @Test
    @DisplayName("案内文に機能名と必要レベルが入る")
    void lockedMessage() {
        String msg = UnlockService.lockedMessage(Feature.SUBSHIP);
        assertThat(msg).contains(Feature.SUBSHIP.label()).contains("Lv15");
    }

    @Test
    @DisplayName("プレイヤーレベルで機能の解放が切り替わる")
    void featureUnlockByLevel() {
        setLevel(4);
        assertThat(UnlockService.isUnlocked(Feature.SHOP)).isFalse();
        setLevel(5);
        assertThat(UnlockService.isUnlocked(Feature.SHOP)).isTrue();
        assertThat(UnlockService.isUnlocked(Feature.MODULE)).isFalse();
    }

    @Test
    @DisplayName("takeNewlyUnlocked は新しく解放された機能だけを返し、2回目は空")
    void takeNewlyUnlocked() {
        setLevel(10);
        assertThat(UnlockService.takeNewlyUnlocked())
                .containsExactlyInAnyOrder(Feature.SHOP, Feature.MODULE, Feature.EXTRA_MODULE);
        assertThat(UnlockService.takeNewlyUnlocked()).isEmpty();
        assertThat(DataManager.getUserData().getProgress().getHighScores()).containsEntry("unlock:seen", 10);

        setLevel(15);
        assertThat(UnlockService.takeNewlyUnlocked()).containsExactly(Feature.SUBSHIP);
        assertThat(UnlockService.takeNewlyUnlocked()).isEmpty();
    }

    @Test
    @DisplayName("guard / refreshAll は未解放ボタンを無効化し、解放後に有効へ戻す")
    void guardButton() {
        setLevel(1);
        JButton button = new JButton("SHOP");
        UnlockService.guard(button, Feature.SHOP);
        assertThat(button.isEnabled()).isFalse();
        assertThat(button.getText()).contains("SHOP").contains("Lv5");

        setLevel(5);
        UnlockService.refreshAll();
        assertThat(button.isEnabled()).isTrue();
        assertThat(button.getText()).isEqualTo("SHOP");
    }
}
