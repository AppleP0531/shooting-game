package com.game.shooting_game.entity;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_ships")
@Data
public class UserShipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shipId;

    private int unitLevel = 1; // 機体レベル
    private int stars = 0; // ★
    private int copies = 0; // 予備の同一機体

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_ship_upgrades", joinColumns = @JoinColumn(name = "ship_entity_id"))
    @MapKeyColumn(name = "upgrade_name")
    @Column(name = "level")
    private Map<String, Integer> upgrades = new HashMap<>();
}
