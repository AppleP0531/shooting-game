package src.entity;

import src.ResourceManager.ResourceKinds;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import lombok.Data;
import jakarta.persistence.*;

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
    @MapKeyColumn(name = "resource_kind")
    @Enumerated(EnumType.STRING)
    @Column(name = "amount")
    private Map<ResourceKinds, Integer> equipment = new HashMap<>();

    // --- 所有機体 --- 
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserShipEntity> ownedShips = new ArrayList<>();
}