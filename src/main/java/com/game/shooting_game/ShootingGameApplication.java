package com.game.shooting_game;

import src.ShootingGameMain;

import javax.swing.SwingUtilities;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ShootingGameApplication {

    public static void main(String[] args) {
        SpringApplication builder = new SpringApplication(ShootingGameApplication.class);
        builder.setHeadless(false);
        ConfigurableApplicationContext context = builder.run(args);

        SwingUtilities.invokeLater(() -> {
            ShootingGameMain.main(args);
        });
    }

}
