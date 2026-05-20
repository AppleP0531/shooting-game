package com.game.shooting_game;

import javax.swing.SwingUtilities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShootingGameApplication {

    public static void main(String[] args) {
        SpringApplication builder = new SpringApplication(ShootingGameApplication.class);
        builder.setHeadless(false);
        try {
            builder.run(args);
        } catch (Exception e) {
        }

        SwingUtilities.invokeLater(() -> {
            ShootingGameMain.setup(args);
        });
    }
}
