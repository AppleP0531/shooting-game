package com.game.shooting_game.domain.cost;

import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;

/**
 * 強化・制作に必要なコストの内訳(コインと素材)
 */
public record Cost(int coins, Map<ResourceKinds, Integer> items) {
}
