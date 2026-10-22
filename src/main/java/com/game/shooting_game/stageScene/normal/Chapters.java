package com.game.shooting_game.stageScene.normal;

/**
 * ノーマルステージの章(20ステージごと)とボスのステージ
 * 章の名前は星座。章の最後のステージ(20, 40, …)に、ステージ選択画面の章ラベルを出す。
 */
public final class Chapters {
    /** 1つの章のステージ数 */
    public static final int STAGES_PER_CHAPTER = 20;
    /** ボスのステージの間隔(5の倍数) */
    public static final int BOSS_INTERVAL = 5;

    private static final String[] NAMES = {
            "牡羊座", "牡牛座", "双子座", "蟹座", "獅子座", "乙女座", "天秤座", "蠍座", "射手座", "山羊座",
            "水瓶座", "魚座" };

    private Chapters() {
    }

    /** ステージの属する章の番号(1から) */
    public static int chapterOf(int stageId) {
        return (Math.max(1, stageId) - 1) / STAGES_PER_CHAPTER + 1;
    }

    /** 章の最後のステージか(章ラベルを出す所) */
    public static boolean isChapterEnd(int stageId) {
        return stageId > 0 && stageId % STAGES_PER_CHAPTER == 0;
    }

    /** ボスのステージか */
    public static boolean isBoss(int stageId) {
        return stageId > 0 && stageId % BOSS_INTERVAL == 0;
    }

    /** 章の名前(星座。12を超えたら最初から繰り返す) */
    public static String name(int chapter) {
        return NAMES[(Math.max(1, chapter) - 1) % NAMES.length];
    }

    /** 章ラベルの文字(例: 第4章・蟹座) */
    public static String label(int chapter) {
        return "第" + chapter + "章・" + name(chapter);
    }
}
