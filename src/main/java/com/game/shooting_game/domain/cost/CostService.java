package com.game.shooting_game.domain.cost;

import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;

/**
 * コストの所持確認と支払い(強化・制作・変換など、すべての画面で共通)
 * 管理者モードではコインと素材が無制限になる。
 */
public final class CostService {
    public static final int UNLIMITED = 999_999_999; // 管理者モードで表示・判定に使う数

    private CostService() {
    }

    /**
     * 指定した素材(レア度込み)の所持数
     */
    public static int owned(ResourceKinds kind) {
        if (DataManager.isAdminMode())
            return UNLIMITED; // 管理者モードは素材が無制限
        return ownedReal(kind);
    }

    /**
     * 実際に持っている素材の数(管理者モードでも実数)
     */
    public static int ownedReal(ResourceKinds kind) {
        UserData ud = DataManager.getUserData();
        return ud == null ? 0 : ud.getResources().getEquipments().getOrDefault(kind, 0);
    }

    /**
     * 表示・判定に使うコインの数(管理者モードは無制限)
     */
    public static int coins() {
        UserData ud = DataManager.getUserData();
        if (ud == null)
            return 0;
        return DataManager.isAdminMode() ? UNLIMITED : ud.getResources().getCoins();
    }

    public static boolean canAfford(Cost cost) {
        UserData ud = DataManager.getUserData();
        if (ud == null)
            return false;
        if (DataManager.isAdminMode())
            return true;
        if (ud.getResources().getCoins() < cost.coins())
            return false;
        for (Map.Entry<ResourceKinds, Integer> e : cost.items().entrySet())
            if (owned(e.getKey()) < e.getValue())
                return false;
        return true;
    }

    /**
     * コストの支払い(コインと素材を減らす。呼ぶ前に canAfford を確認すること)
     */
    public static void pay(UserData ud, Cost cost) {
        if (DataManager.isAdminMode())
            return; // 管理者モードは何も減らさない
        ud.getResources().setCoins(ud.getResources().getCoins() - cost.coins());
        Map<ResourceKinds, Integer> eq = ud.getResources().getEquipments();
        for (Map.Entry<ResourceKinds, Integer> e : cost.items().entrySet()) {
            int left = eq.getOrDefault(e.getKey(), 0) - e.getValue();
            if (left <= 0)
                eq.remove(e.getKey());
            else
                eq.put(e.getKey(), left);
        }
    }
}
