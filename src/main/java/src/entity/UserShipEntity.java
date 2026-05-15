package src.entity;

import java.util.Map;
import java.util.HashMap;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "user_ships")
@Data
public class UserShipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shipId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_ship_upgrades", joinColumns = @JoinColumn(name = "ship_entity_id"))
    @MapKeyColumn(name = "upgrade_name")
    @Column(name = "level")
    private Map<String, Integer> upgrades = new HashMap<>();
}
