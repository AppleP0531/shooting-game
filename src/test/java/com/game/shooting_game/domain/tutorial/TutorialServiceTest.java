package com.game.shooting_game.domain.tutorial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.tutorial.TutorialService.Guide;

class TutorialServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.create("tester");
    }

    @Test
    @DisplayName("設定: IDが重複せず、きっかけは scene か event のどちらか一方で、ページがある")
    void configIsConsistent() {
        Set<String> ids = new HashSet<>();
        assertThat(TutorialService.all()).isNotEmpty();
        for (Guide g : TutorialService.all()) {
            assertThat(ids.add(g.id())).as("IDの重複: " + g.id()).isTrue();
            assertThat((g.scene() == null) != (g.event() == null)).as(g.id() + " のきっかけ").isTrue();
            assertThat(g.pages()).as(g.id() + " のページ").isNotEmpty();
            assertThat(g.title()).isNotBlank();
        }
    }

    @Test
    @DisplayName("設定の scene は、実在する画面(SceneType)の名前")
    void sceneNamesExist() {
        Set<String> scenes = new HashSet<>();
        for (SceneType t : SceneType.values())
            scenes.add(t.name());
        for (Guide g : TutorialService.all())
            if (g.scene() != null)
                assertThat(scenes).as(g.id()).contains(g.scene());
    }

    @Test
    @DisplayName("ステージ1の完全ガイド(開始・敵・アイテム・被弾)が揃っている")
    void stage1Guides() {
        for (String e : new String[] { "stage1.start", "stage1.enemy", "stage1.item", "stage1.hit" })
            assertThat(TutorialService.forEvent(e)).as(e).isNotNull();
    }

    @Test
    @DisplayName("見たガイドは、もう出ない。リセットすると、また出る")
    void seenAndReset() {
        Guide g = TutorialService.forScene("LOBBY");
        assertThat(g).isNotNull();
        assertThat(TutorialService.seen(g)).isFalse();

        TutorialService.markSeen(g);
        assertThat(TutorialService.seen(g)).isTrue();
        assertThat(TutorialService.forScene("LOBBY")).isNull();

        assertThat(TutorialService.reset()).isEqualTo(1);
        assertThat(TutorialService.forScene("LOBBY")).isNotNull();
    }

    @Test
    @DisplayName("見た記録は★の数(mission:)に混ざらない")
    void doesNotAffectStars() {
        Guide g = TutorialService.forScene("LOBBY");
        TutorialService.markSeen(g);
        var hs = DataManager.getUserData().getProgress().getHighScores();
        assertThat(hs.keySet().stream().filter(k -> k.startsWith("mission:"))).isEmpty();
    }

    @Test
    @DisplayName("見ていない数が、見るたびに減る。知らない画面・できごとは null")
    void unseenCountAndUnknown() {
        int before = TutorialService.unseenCount();
        TutorialService.markSeen(TutorialService.forScene("SHOP"));
        assertThat(TutorialService.unseenCount()).isEqualTo(before - 1);
        assertThat(TutorialService.forScene("NOPE")).isNull();
        assertThat(TutorialService.forEvent("nope")).isNull();
    }
}
