package com.game.shooting_game.utils;

import java.util.HashMap;
import java.util.Map;

public class LogUtils {
    // ログ関連
    private static final boolean ENABLE_LOG = true; // ログ全体のON/OFF
    private static final boolean ENABLE_DEV_LOG = true; // 開発用ログのON/OFF
    private static final boolean ENABLE_DEBUG_LOG = true; // デバッグログのON/OFF
    private static final boolean ENABLE_DEBUG_INFO_LOG = true; // デバッグ用情報ログのON/OFF
    private static final boolean ENABLE_INFO_LOG = true; // 情報ログのON/OFF
    private static final boolean ENABLE_WARN_LOG = true; // 警告ログのON/OFF
    private static final boolean ENABLE_ERROR_LOG = true; // エラーログのON/OFF
    private static final boolean ENABLE_FATAL_LOG = true; // 致命的ログのON/OFF
    private static final boolean ENABLE_STACKTRACE_LOG = true; // スタックトレースログのON/OFF

    /**
     * ログレベル列挙型
     */
    private enum LogLevel {
        // タグ名と、どのフラグを見るかを定義
        DEBUG("[DEBUG]", ANSI.CYAN),
        INFO("[INFO]", ANSI.GREEN),
        WARN("[WARN]", ANSI.YELLOW),
        ERROR("[ERROR]", ANSI.RED),
        FATAL("[FATAL]", ANSI.BOLD + ANSI.RED);

        private final String tag;
        private final String color;

        private LogLevel(String tag, String color) {
            this.tag = tag;
            this.color = color;
        }

        // 色を返す
        public String getDecoratedTag() {
            return color;
        }

        // 文字列タグからEnumを探すMap
        private static final Map<String, LogLevel> TAG_MAP = new HashMap<>();
        static {
            for (LogLevel lv : values())
                TAG_MAP.put(lv.tag, lv);
        }

        public static LogLevel fromTag(String tag) {
            return TAG_MAP.get(tag);
        }

        // このログレベルが表示対象かどうかを判定する
        public boolean isEnabled() {
            // マスタースイッチがオフならすべて表示しない
            if (!ENABLE_LOG)
                return false;

            return switch (this) {
                // DEBUGの時だけ、DEBUGフラグとDEVフラグの両方をチェックする
                case DEBUG -> ENABLE_DEBUG_LOG && ENABLE_DEV_LOG;
                case INFO -> ENABLE_INFO_LOG;
                case WARN -> ENABLE_WARN_LOG;
                case ERROR -> ENABLE_ERROR_LOG;
                case FATAL -> ENABLE_FATAL_LOG;
            };
        }
    }

    /**
     * ログ出力メソッド
     * 
     * @param tag       タグ名
     * @param className 呼び出しクラス名
     * @param msg       メッセージ
     */
    public static void systemLog(String tag, String className, String msg) {
        // 表示すべきかどうかを判定
        LogLevel level = LogLevel.fromTag(tag);
        if (!level.isEnabled())
            return;

        // メインクラスの時だけ太字にする
        String classNameDec = className.equals("[MAIN]") ? (ANSI.CYAN + ANSI.BOLD) : ANSI.CYAN;

        System.out.printf("%s%-8s%s : %s%-25s%s : %s%n",
                level.getDecoratedTag(), tag, ANSI.RESET, // タグ部分
                classNameDec, className, ANSI.RESET, // クラス名部分
                msg // メッセージ
        );
    }

    /**
     * スタックトレース付きログ出力メソッド
     * 
     * @param tag       タグ名
     * @param component 呼び出しクラス名
     * @param message   メッセージ
     * @param e         スタックトレース
     */
    public static void systemLog(String tag, String component, String message, Exception e) {
        // 通常のログを出力
        systemLog(tag, component, message);

        // スタックトレースの表示判定
        if (ENABLE_STACKTRACE_LOG && e != null)
            e.printStackTrace();

        // fatalエラーの場合プログラムの強制終了
        if (tag == "[FATAL]")
            System.exit(1);
    }

    /**
     * デバッグ用ログ出力
     * 
     * @param message
     */
    public static void debugLog(Object message) {
        if (ENABLE_DEBUG_INFO_LOG) {
            StackTraceElement e = Thread.currentThread().getStackTrace()[2];
            System.out.printf("[%s:%d] %s%n", e.getFileName(), e.getLineNumber(), message);
        }
    }
}
