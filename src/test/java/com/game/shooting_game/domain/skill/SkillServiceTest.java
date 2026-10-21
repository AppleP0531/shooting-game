package com.game.shooting_game.domain.skill;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;

class SkillServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
        SkillService.invalidate();
    }

    @AfterEach
    void tearDown() {
        DataManager.setAdminMode(false);
    }

    private UserData.ShipStats stats() {
        UserData ud = DataManager.getUserData();
        return ud.getShips().get(ud.getSelectedShip());
    }

    /** 1時間後に終わる研究を直接セットする */
    private void startResearch(String skill) {
        stats().setResearchSkill(skill);
        stats().setResearchFinishAt(System.currentTimeMillis() + 3_600_000L);
    }

    private void tickets(int n) {
        DataManager.getUserData().getProgress().getHighScores().put(ShopService.key(ItemKinds.SKIP_TICKET), n);
    }

    @Test
    @DisplayName("研究中でなければスキップできず、券も減らない")
    void notResearching() {
        tickets(2);
        assertThat(SkillService.skipResearchWithTicket()).isFalse();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(2);
    }

    @Test
    @DisplayName("研究中に券があれば、券が1枚減って研究が完了しレベルが上がる")
    void skipWithTicket() {
        tickets(2);
        startResearch("Power");
        assertThat(SkillService.isResearching()).isTrue();
        assertThat(SkillService.skipResearchWithTicket()).isTrue();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(1);
        assertThat(SkillService.isResearching()).isFalse();
        assertThat(SkillService.getLevel("Power")).isEqualTo(1);
    }

    @Test
    @DisplayName("券が無ければスキップできず、研究もそのまま")
    void noTicket() {
        tickets(0);
        startResearch("Power");
        assertThat(SkillService.skipResearchWithTicket()).isFalse();
        assertThat(SkillService.isResearching()).isTrue();
        assertThat(SkillService.getLevel("Power")).isZero();
    }

    @Test
    @DisplayName("管理者モードは券を消費せずスキップできる")
    void adminSkipsFree() {
        DataManager.setAdminMode(true);
        tickets(0);
        startResearch("Power");
        assertThat(SkillService.skipResearchWithTicket()).isTrue();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isZero();
        assertThat(SkillService.getLevel("Power")).isEqualTo(1);
    }

    @Test
    @DisplayName("describe: 減る効果(Slim, Lucky)は「-」、ほかは「+」")
    void describeSign() {
        String slim = SkillService.describe(SkillService.find("Slim"));
        String lucky = SkillService.describe(SkillService.find("Lucky"));
        String power = SkillService.describe(SkillService.find("Power"));
        assertThat(slim).contains("Lvごとに -").doesNotContain("Lvごとに +");
        assertThat(lucky).contains("Lvごとに -").doesNotContain("Lvごとに +");
        assertThat(power).contains("Lvごとに +").doesNotContain("Lvごとに -");
    }

    @Test
    @DisplayName("setLevel は0〜最大レベルに収めて設定し、0で記録を消す")
    void setLevelClamps() {
        var d = SkillService.getConfig().stream().filter(x -> x.getMaxLevel() > 1).findFirst().orElseThrow();
        assertThat(SkillService.setLevel(d, 1)).isEqualTo(1);
        assertThat(SkillService.getLevel(d.getName())).isEqualTo(1);
        assertThat(SkillService.setLevel(d, 9999)).isEqualTo(d.getMaxLevel());
        assertThat(SkillService.getLevel(d.getName())).isEqualTo(d.getMaxLevel());
        assertThat(SkillService.setLevel(d, -3)).isZero();
        assertThat(stats().getUpgrades()).doesNotContainKey(d.getName());
    }

    @Test
    @DisplayName("findIgnoreCase は大文字小文字を区別せず、なければnull")
    void findIgnoreCase() {
        String name = SkillService.getConfig().get(0).getName();
        assertThat(SkillService.findIgnoreCase(name.toUpperCase())).isNotNull();
        assertThat(SkillService.findIgnoreCase(name.toLowerCase()).getName()).isEqualTo(name);
        assertThat(SkillService.findIgnoreCase("noSuchSkill")).isNull();
    }
}
