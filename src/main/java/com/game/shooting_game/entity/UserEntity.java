package com.game.shooting_game.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.game.shooting_game.ResourceManager.ResourceKinds;

@Entity
@Table(name = "users")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    // --- 基本情報 ---
    private String lastPlayTime;
    private String totalPlayTime;
    private String selectedShip;
    private String subShip;

    // --- Progress ---
    private int exp;
    private int clearedStage;

    // --- ステージごとのハイスコア ---
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_high_scores", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "stage_key")
    @Column(name = "score")
    private Map<String, Integer> highScores = new HashMap<>();

    // --- Resources ---
    private int coins;

    // --- 所有資源 ---
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_equipments", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "amount")
    private Map<ResourceKinds, Integer> equipments = new HashMap<>();

    // --- 所有機体 ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserShipEntity> ownedShips = new ArrayList<>();
}