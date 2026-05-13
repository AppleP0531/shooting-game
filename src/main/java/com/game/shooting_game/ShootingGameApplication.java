package com.game.shooting_game;

import src.ShootingGameMain;

import javax.swing.SwingUtilities;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "src")
@EnableJpaRepositories(basePackages = "src.repository")
@EntityScan(basePackages = "src.entity")
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
