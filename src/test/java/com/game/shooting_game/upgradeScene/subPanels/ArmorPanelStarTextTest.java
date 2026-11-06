package com.game.shooting_game.upgradeScene.subPanels;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.domain.armor.ArmorService;

class ArmorPanelStarTextTest {

    @Test
    @DisplayName("starText は、最大まで並べ、上げた分だけ塗りつぶす")
    void starText() {
        assertThat(ArmorPanel.starText(0)).isEqualTo("☆☆☆☆☆");
        assertThat(ArmorPanel.starText(2)).isEqualTo("★★☆☆☆");
        assertThat(ArmorPanel.starText(ArmorService.MAX_STARS)).isEqualTo("★★★★★");
    }
}
