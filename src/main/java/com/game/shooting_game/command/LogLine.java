package com.game.shooting_game.command;

/**
 * コマンド・チャット欄の1行
 *
 * @param text      表示する文章
 * @param kind      種別(色や行頭の記号が変わる)
 * @param createdAt 追加された時刻(エポックミリ秒。入力欄を閉じたあとの表示と、薄く消える動きに使う)
 */
record LogLine(String text, Kind kind, long createdAt) {
    /** 行の種別 */
    enum Kind {
        INFO, // 通常のお知らせ(白)
        SUCCESS, // 成功(緑)
        ERROR, // 失敗・警告(赤)
        HELP, // 使い方・ヘルプ(水色)
        CHAT // ユーザーの発言(「名前 文章」の形で白)
    }

    LogLine(String text, Kind kind) {
        this(text, kind, System.currentTimeMillis());
    }
}
