package com.game.shooting_game.domain.convert;

import com.game.shooting_game.domain.cost.CostService;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * 素材の変換(同じ素材の同じレア度5個 → 1つ上のレア度1個)
 * 必要な素材のレア度が足りないときに、余った下位の素材から作れるようにするための仕組み。
 * 上のレア度から下には変換できない。
 */
public final class ConvertService {
    private static final String component = "[ConvertService]";

    /** 変換に必要な個数 */
    public static final int RATE = 5;

    private ConvertService() {
    }

    /**
     * 1つ上のレア度(最上位ならnull)
     */
    public static Rarity next(Rarity r) {
        Rarity[] all = Rarity.values();
        int i = r.ordinal() + 1;
        return i < all.length ? all[i] : null;
    }

    /**
     * 今、何回変換できるか
     */
    public static int maxTimes(ResourceName name, Rarity r) {
        if (next(r) == null)
            return 0;
        // 実際に持っている数で数える(管理者モードの無制限の数では変換できない)
        return CostService.ownedReal(new ResourceKinds(name, r)) / RATE;
    }

    /**
     * 素材の変換
     * 
     * @param name  素材
     * @param from  変換元のレア度
     * @param times 回数(変換できる回数を超えるときは、できる分だけ)
     * @return 変換した回数
     */
    public static synchronized int convert(ResourceName name, Rarity from, int times) {
        UserData ud = DataManager.getUserData();
        Rarity to = next(from);
        if (ud == null || to == null || times <= 0)
            return 0;

        int n = Math.min(times, maxTimes(name, from));
        if (n <= 0)
            return 0;

        Map<ResourceKinds, Integer> eq = ud.getResources().getEquipments();
        ResourceKinds fromKey = new ResourceKinds(name, from);
        ResourceKinds toKey = new ResourceKinds(name, to);

        int left = eq.getOrDefault(fromKey, 0) - n * RATE;
        if (left <= 0)
            eq.remove(fromKey);
        else
            eq.put(fromKey, left);
        eq.merge(toKey, n, Integer::sum);

        DataManager.save();
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + name + ANSI.RESET + " " + from + " x" + (n * RATE) + " -> " + to + " x" + n + ".");
        return n;
    }
}
