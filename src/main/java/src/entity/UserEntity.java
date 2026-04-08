package src.entity;

import src.ResourceManager.ResourceKinds;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import lombok.Data;
import jakarta.persistence.*;

/**
 * ユーザーデータのエンティティクラス
 */
@Entity
@Table(name = "users")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 基本情報 ---
    private String lastPlayTime;
    private String totalPlayTime;
    private String selectedShip;
    private String subShip;

    // --- Progress ---
    private int exp;
    private int clearedStage;

    // ステージごとのハイスコア (JSONの progress.highScores)
    @ElementCollection
    @CollectionTable(name = "user_high_scores", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "stage_key")
    @Column(name = "score")
    private Map<String, Integer> highScores = new HashMap<>();

    // --- Resources ---
    private int coins;

    // 所有資源 (JSONの resources.equipment)
    @ElementCollection
    @CollectionTable(name = "user_equipment", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "resource_kind")
    @Enumerated(EnumType.STRING)
    @Column(name = "amount")
    private Map<ResourceKinds, Integer> equipment = new HashMap<>();

    // --- Ships (機体強化情報(JSONの ships.ShipID.upgrades.UpgradeName : Level)) ---
    @ElementCollection
    @CollectionTable(name = "user_ship_upgrades", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "ship_upgrade_key") // 例: "Ship5:Origin"
    @Column(name = "level")
    private Map<String, Integer> shipUpgrades = new LinkedHashMap<>();
}