package com.game.shooting_game.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 端末ごとの設定(FPSなど)
 * ログインに依存せず、ゲームの実行フォルダの settings.properties に保存する。読み書きに失敗したら既定値で動く。
 */
public final class GameSettings {
    private static final String component = "[GameSettings]";
    private static final Path FILE = Path.of("settings.properties");

    public static final int[] FPS_CHOICES = { 30, 60, 120, 144, 240 };
    public static final int DEFAULT_FPS = 120;

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    private GameSettings() {
    }

    /**
     * 保存されているFPS(選べる値でなければ既定値)
     */
    public static int getFps() {
        load();
        try {
            int fps = Integer.parseInt(props.getProperty("fps", String.valueOf(DEFAULT_FPS)));
            return fps > 0 ? fps : DEFAULT_FPS;
        } catch (NumberFormatException e) {
            return DEFAULT_FPS;
        }
    }

    /**
     * FPSを保存する
     */
    public static void setFps(int fps) {
        load();
        props.setProperty("fps", String.valueOf(fps));
        save();
    }

    /**
     * ダメージ数字を出すか(既定はON)
     */
    public static boolean isDamageTextEnabled() {
        load();
        return Boolean.parseBoolean(props.getProperty("damageText", "true"));
    }

    /**
     * ダメージ数字を出すかを保存する
     */
    public static void setDamageTextEnabled(boolean enabled) {
        load();
        props.setProperty("damageText", String.valueOf(enabled));
        save();
    }

    private static synchronized void load() {
        if (loaded)
            return;
        loaded = true;
        if (!Files.exists(FILE))
            return;
        try (InputStream in = Files.newInputStream(FILE)) {
            props.load(in);
        } catch (IOException e) {
            LogUtils.systemLog("[WARN]", component, "Failed to read " + FILE + ". Using default settings.");
        }
    }

    private static synchronized void save() {
        try (OutputStream out = Files.newOutputStream(FILE)) {
            props.store(out, "ShootingGame settings");
        } catch (IOException e) {
            LogUtils.systemLog("[WARN]", component, "Failed to write " + FILE + ".");
        }
    }
}
