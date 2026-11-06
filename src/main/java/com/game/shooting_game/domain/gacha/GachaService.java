package com.game.shooting_game.domain.gacha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;

/**
 * 機体のガチャ(コインで引くと、ランダムな機体のピースが手に入る)
 * <p>
 * - 1回で、機体を1つ選び、その機体のピースを1〜5個もらう。10連は、少し安く、3個以上が最低1回は出る。
 * - 機体の選ばれやすさは、入手に必要なコイン(ShipConfig の cost)が高いほど低い。
 * - ピースが {@link PieceService#UNLOCK_PIECES} 個たまり、コインを払うと、機体を解放できる。
 * 純粋な部分(選び方)は static メソッドに分けてあり、乱数を渡せる。
 */
public final class GachaService {
    /** 1回のコイン */
    public static final int SINGLE_COST = 1000;
    /** 10連のコイン */
    public static final int TEN_COST = 9000;

    // 1回で手に入るピースの数と、その出やすさ
    private static final int[] PIECE_COUNTS = { 1, 2, 3, 5 };
    private static final int[] PIECE_WEIGHTS = { 60, 25, 10, 5 };
    /** 10連で最低1回は出る、ピースの数 */
    public static final int TEN_GUARANTEE_PIECES = 3;

    /** 1回の結果 */
    public record Result(String shipId, int pieces) {
    }

    private GachaService() {
    }

    /**
     * 機体の選ばれやすさ(入手コインが高いほど低い。コイン0の機体は最も出やすい)
     */
    public static double weight(int shipCost) {
        return 100.0 / (1 + Math.log10(Math.max(1, shipCost) / 1000.0 + 1) * 2);
    }

    /**
     * 機体を1つ選ぶ(重み付き)
     *
     * @param costs 機体ID → 入手に必要なコイン(IDの順に並んだもの)
     */
    public static String pickShip(Random rnd, Map<String, Integer> costs) {
        double total = 0;
        for (int c : costs.values())
            total += weight(c);
        double v = rnd.nextDouble() * total;
        String last = null;
        for (Map.Entry<String, Integer> e : costs.entrySet()) {
            last = e.getKey();
            v -= weight(e.getValue());
            if (v <= 0)
                return last;
        }
        return last;
    }

    /**
     * ピースの数を選ぶ(1〜5)
     */
    public static int pickPieces(Random rnd) {
        int total = 0;
        for (int w : PIECE_WEIGHTS)
            total += w;
        int v = rnd.nextInt(total);
        for (int i = 0; i < PIECE_WEIGHTS.length; i++) {
            v -= PIECE_WEIGHTS[i];
            if (v < 0)
                return PIECE_COUNTS[i];
        }
        return PIECE_COUNTS[0];
    }

    /**
     * 引く(コインやピースは動かさない。結果だけ返す)
     *
     * @param times 回数(10なら、3個以上が最低1回出る)
     */
    public static List<Result> roll(Random rnd, Map<String, Integer> costs, int times) {
        List<Result> out = new ArrayList<>();
        boolean guaranteed = false;
        for (int i = 0; i < times; i++) {
            int pieces = pickPieces(rnd);
            // 10連の最後の1回は、それまでに3個以上が出ていなければ、3個以上にする
            if (times >= 10 && i == times - 1 && !guaranteed && pieces < TEN_GUARANTEE_PIECES)
                pieces = TEN_GUARANTEE_PIECES;
            if (pieces >= TEN_GUARANTEE_PIECES)
                guaranteed = true;
            out.add(new Result(pickShip(rnd, costs), pieces));
        }
        return out;
    }

    /**
     * ガチャに出る機体と、その入手コイン(ShipConfig から。IDの順)
     */
    public static Map<String, Integer> shipCosts() {
        Map<String, Integer> costs = new TreeMap<>();
        var ships = ShipManager.getInstance().getShipConfig().get(ShipKinds.PLAYER);
        if (ships != null)
            for (var e : ships.entrySet())
                if (e.getValue().getDatas() != null && e.getKey().startsWith("Ship"))
                    costs.put(e.getKey(), e.getValue().getDatas().getCost());
        return costs;
    }

    // ----- コインを払って引く -----
    /**
     * 引く(コインを払い、ピースを増やす)
     *
     * @param times 1 または 10
     * @return 結果(コインが足りなければ空)
     */
    public static List<Result> pull(int times) {
        UserData ud = DataManager.getUserData();
        int price = times >= 10 ? TEN_COST : SINGLE_COST;
        Map<String, Integer> costs = shipCosts();
        if (ud == null || costs.isEmpty() || (!DataManager.isAdminMode() && CostService.coins() < price))
            return List.of();
        if (!DataManager.isAdminMode())
            ud.getResources().setCoins(ud.getResources().getCoins() - price);
        List<Result> results = roll(new Random(), costs, times);
        for (Result r : results)
            PieceService.add(r.shipId(), r.pieces());
        return results;
    }

    // ----- 機体の解放 -----
    /** 機体を解放できるか(未所持で、ピースとコインが足りている) */
    public static boolean canUnlock(String shipId) {
        UserData ud = DataManager.getUserData();
        if (ud == null || ud.getShips().containsKey(shipId))
            return false;
        return PieceService.owned(shipId) >= PieceService.UNLOCK_PIECES
                && (DataManager.isAdminMode() || CostService.coins() >= shipCost(shipId));
    }

    /** 機体を解放する(ピースとコインを払う。管理者モードはコインを払わない) */
    public static boolean unlock(String shipId) {
        UserData ud = DataManager.getUserData();
        if (!canUnlock(shipId))
            return false;
        PieceService.consume(shipId, PieceService.UNLOCK_PIECES);
        if (!DataManager.isAdminMode())
            ud.getResources().setCoins(ud.getResources().getCoins() - shipCost(shipId));
        ud.unlockShip(shipId);
        return true;
    }

    /** 機体の解放に必要なコイン */
    public static int shipCost(String shipId) {
        return shipCosts().getOrDefault(shipId, 0);
    }
}
