package com.game.shooting_game.utils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * BSD syslog(RFC 3164)に沿ったログ出力。
 * <p>
 * 1行は {@code <PRI>Mmm dd hh:mm:ss HOSTNAME TAG[PID]: MSG} の形で、
 * PRI = Facility × 8 + Severity。出力先はコンソールのみ(ネットワーク送信はしない)。
 * </p>
 */
public class LogUtils {
    private static final boolean ENABLE_LOG = true; // ログ全体のON/OFF
    private static final boolean ENABLE_STACKTRACE_LOG = true; // スタックトレースログのON/OFF

    /** 出力する最も軽い重大度(これより数値が大きい=軽いものは捨てる。syslog の閾値と同じ考え方) */
    private static volatile Severity minSeverity = Severity.DEBUG;

    /** RFC 3164 の TAG は32文字まで */
    private static final int MAX_TAG_LENGTH = 32;

    /** RFC 3164 の TIMESTAMP("Oct  5 09:03:01" のように日が1桁なら空白で埋める) */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("MMM ppd HH:mm:ss", Locale.ENGLISH);

    private static final String HOSTNAME = resolveHostname();
    private static final long PID = ProcessHandle.current().pid();

    /**
     * Facility(メッセージの発生源)。このゲームでは USER と LOCAL0〜7 だけを使う。
     */
    public enum Facility {
        USER(1),
        LOCAL0(16), LOCAL1(17), LOCAL2(18), LOCAL3(19),
        LOCAL4(20), LOCAL5(21), LOCAL6(22), LOCAL7(23);

        private final int code;

        private Facility(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    /**
     * Severity(重大度。数値が小さいほど重大)。
     */
    public enum Severity {
        EMERGENCY(0, "FATAL", ANSI.BOLD + ANSI.RED), // システムが使えない
        ALERT(1, "FATAL", ANSI.BOLD + ANSI.RED), // 直ちに対処が必要
        CRITICAL(2, "FATAL", ANSI.BOLD + ANSI.RED), // 致命的(続行できない)
        ERROR(3, "ERROR", ANSI.RED), // エラー
        WARNING(4, "WARN", ANSI.YELLOW), // 警告(既定値で続行するなど)
        NOTICE(5, "NOTICE", ANSI.GREEN), // 正常だが注目すべき出来事
        INFO(6, "INFO", ANSI.GREEN), // 通常の情報
        DEBUG(7, "DEBUG", ANSI.CYAN); // デバッグ

        private final int code;
        private final String label;
        private final String color;

        private Severity(int code, String label, String color) {
            this.code = code;
            this.label = label;
            this.color = color;
        }

        public int code() {
            return code;
        }

        /** 画面に出すラベル(数字では INFO か DEBUG か分からないため。EMERGENCY〜CRITICAL は FATAL) */
        public String label() {
            return label;
        }
    }

    /** このゲームの既定の Facility */
    private static final Facility DEFAULT_FACILITY = Facility.USER;

    private LogUtils() {
    }

    // ---- 設定 ----

    /** これより軽い(数値が大きい)ログは出さない */
    public static void setMinSeverity(Severity severity) {
        minSeverity = severity;
    }

    public static Severity getMinSeverity() {
        return minSeverity;
    }

    /** 出力対象かどうか */
    public static boolean isLoggable(Severity severity) {
        return ENABLE_LOG && severity.code() <= minSeverity.code();
    }

    /** PRI の値(Facility × 8 + Severity) */
    public static int priority(Facility facility, Severity severity) {
        return facility.code() * 8 + severity.code();
    }

    // ---- 呼び出し用のAPI ----

    public static void debug(String component, String msg) {
        log(DEFAULT_FACILITY, Severity.DEBUG, component, msg);
    }

    public static void info(String component, String msg) {
        log(DEFAULT_FACILITY, Severity.INFO, component, msg);
    }

    public static void notice(String component, String msg) {
        log(DEFAULT_FACILITY, Severity.NOTICE, component, msg);
    }

    public static void warn(String component, String msg) {
        log(DEFAULT_FACILITY, Severity.WARNING, component, msg);
    }

    public static void error(String component, String msg) {
        log(DEFAULT_FACILITY, Severity.ERROR, component, msg);
    }

    /** スタックトレース付きのエラー */
    public static void error(String component, String msg, Exception e) {
        log(DEFAULT_FACILITY, Severity.ERROR, component, msg);
        printStackTrace(e);
    }

    /** 致命的エラー。ログを出してプログラムを強制終了する */
    public static void fatal(String component, String msg, Exception e) {
        log(DEFAULT_FACILITY, Severity.CRITICAL, component, msg);
        printStackTrace(e);
        System.exit(1);
    }

    /**
     * ログを1行出す。
     *
     * @param facility  発生源
     * @param severity  重大度
     * @param component 呼び出し元(TAG になる。前後の [ ] は外す)
     * @param msg       メッセージ
     */
    public static void log(Facility facility, Severity severity, String component, String msg) {
        if (!isLoggable(severity))
            return;

        System.out.println(format(facility, severity, LocalDateTime.now(), component, msg));
    }

    /**
     * デバッグ用ログ出力。TAG は呼び出し元の「ファイル名:行」になる。
     */
    public static void debugLog(Object message) {
        if (!isLoggable(Severity.DEBUG))
            return;

        StackTraceElement e = Thread.currentThread().getStackTrace()[2];
        log(DEFAULT_FACILITY, Severity.DEBUG, e.getFileName() + ":" + e.getLineNumber(), String.valueOf(message));
    }

    /**
     * デバッグ用ログ出力(フォーマッタ付き)。末尾の改行は取り除く。
     */
    public static void debugLog(String format, Object... messages) {
        if (!isLoggable(Severity.DEBUG))
            return;

        StackTraceElement e = Thread.currentThread().getStackTrace()[2];
        String msg = String.format(format, messages).stripTrailing();
        log(DEFAULT_FACILITY, Severity.DEBUG, e.getFileName() + ":" + e.getLineNumber(), msg);
    }

    // ---- 整形 ----

    /**
     * RFC 3164 形式の1行を作る(色つき。テストしやすいよう時刻は引数で受ける)。
     */
    static String format(Facility facility, Severity severity, LocalDateTime time, String component, String msg) {
        // 行頭は PRI の数字ではなく [INFO] などのラベルにする(PRI は priority() で計算できる)
        return String.format("%s%-8s%s%s %s %s[%d]: %s",
                severity.color, "[" + severity.label() + "]", ANSI.RESET,
                TIMESTAMP.format(time),
                HOSTNAME,
                ANSI.CYAN + toTag(component) + ANSI.RESET, PID,
                msg);
    }

    /** TAG にする: 前後の [ ] を外し、32文字までにする */
    static String toTag(String component) {
        String tag = component == null ? "-" : component.strip();
        if (tag.startsWith("["))
            tag = tag.substring(1);
        if (tag.endsWith("]"))
            tag = tag.substring(0, tag.length() - 1);
        if (tag.isEmpty())
            tag = "-";
        return tag.length() > MAX_TAG_LENGTH ? tag.substring(0, MAX_TAG_LENGTH) : tag;
    }

    private static void printStackTrace(Exception e) {
        if (ENABLE_STACKTRACE_LOG && e != null)
            e.printStackTrace();
    }

    private static String resolveHostname() {
        try {
            String name = InetAddress.getLocalHost().getHostName();
            return name == null || name.isBlank() ? "localhost" : name.replace(' ', '-');
        } catch (Exception e) {
            return "localhost";
        }
    }
}
