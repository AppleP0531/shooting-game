package com.game.shooting_game.domain.module;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 所有しているモジュールと、装着の状況(DBには1つの文字列(JSON)として保存する)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInventory {
    public static final int SLOTS = 4;

    private long nextId = 1;
    private List<Module> owned = new ArrayList<>();
    private List<Long> slots = new ArrayList<>(List.of(0L, 0L, 0L, 0L)); // 装着中のモジュールID(0は空き)
}
