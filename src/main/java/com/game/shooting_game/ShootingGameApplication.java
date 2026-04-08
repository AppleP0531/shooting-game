package com.game.shooting_game;

import src.ShootingGameMain;

import javax.swing.SwingUtilities;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ShootingGameApplication {

    public static void main(String[] args) {
        // 1. Spring Bootの起動（HeadlessモードをオフにするのがGUIアプリのコツです）
        SpringApplication builder = new SpringApplication(ShootingGameApplication.class);
        builder.setHeadless(false);
        ConfigurableApplicationContext context = builder.run(args);

        // 2. Swingの画面を起動
        SwingUtilities.invokeLater(() -> {
            // ShootingGameMainのmainメソッドを呼び出すか、
            // 直接インスタンス化して表示処理を行います
            ShootingGameMain.main(args);
        });
    }

}
