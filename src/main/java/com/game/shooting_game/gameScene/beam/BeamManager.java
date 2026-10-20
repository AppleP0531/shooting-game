package com.game.shooting_game.gameScene.beam;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.DamageCalculator;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.Beam.State;
import com.game.shooting_game.gameScene.beam.Beam.Team;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.Vector2;

/**
 * ビームの管理
 * ステージのギミックビームの発射、全ビームの更新・当たり判定・描画を行う。
 * 
 * バランスの考え方:
 * - 敵のビームは予兆(warn)があり、自機と同じ色なら当たらない。1回のダメージは固定で、被弾後の無敵時間も効く。
 * - 自機のビームはダメージを1秒あたりで持ち、一定間隔でしか撃てない。ボスには倍率(bossRate)で控えめに入る。
 */
public final class BeamManager implements Manager {
    private static final double TICK = 0.1; // 自機のビームのダメージ判定の間隔(秒)

    private static BeamManager instance;

    private final ShootingGameMain MAIN;
    private final ShipManager SHIP_M;

    private final List<Beam> beams = new ArrayList<>();
    private final List<BeamEmitter> gimmicks = new ArrayList<>(); // 今のウェーブのギミック
    private final List<BeamBlock> blocks = new ArrayList<>(); // ビームを撃つ四角ブロック
    private double tickTimer = 0;

    private BeamManager() {
        BeamManager.instance = this;
        MAIN = ShootingGameMain.getInstance();
        SHIP_M = ShipManager.getInstance();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static BeamManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, BeamManager::new);
    }

    @Override
    public void setup() {
        beams.clear();
        gimmicks.clear();
        blocks.clear();
        tickTimer = 0;
    }

    /**
     * ビームをすべて消す
     */
    public void clear() {
        beams.clear();
    }

    /**
     * 敵・ボス・ギミックのビームだけを消す(自機のビームは残す)
     */
    public void clearHostile() {
        beams.removeIf(b -> b.getTeam() == Team.HOSTILE);
    }

    /**
     * ウェーブのギミックの設定(前のウェーブの予定は破棄する)
     * 
     * @param specs ギミック(nullまたは空なら何もなし)
     */
    public void setGimmicks(BeamSpec[] specs) {
        gimmicks.clear();
        if (specs == null)
            return;
        for (BeamSpec spec : specs)
            gimmicks.add(new BeamEmitter(spec));
    }

    /**
     * ウェーブのビームブロックの追加
     * 前のウェーブのブロックは消さず、画面の下へ流れて自然に出ていく。
     * 
     * @param specs ブロック(nullまたは空なら何もしない)
     */
    public void setBlocks(BlockSpec[] specs) {
        if (specs == null)
            return;
        for (BlockSpec spec : specs)
            blocks.add(new BeamBlock(spec));
    }

    /**
     * 弾がブロックに当たっているか
     */
    public boolean blocksBullet(Vector2 pos, double radius) {
        for (BeamBlock b : blocks)
            if (b.hits(pos, radius))
                return true;
        return false;
    }

    /**
     * 船をブロックの外へ押し出す(ブロックは通り抜けられない。ステージ中だけ)
     * 画面の端に押し付けられて押し出せないときは、押し潰しになる(動かさない)。
     * 同じブロックによる押し潰しは、めり込みが解けるまで1回だけ数える。
     * 
     * @param ship   船
     * @param radius 船の体の半径(画面倍率込み)
     * @param bounds 船が動ける範囲(nullなら制限なし)
     * @return 新しく押し潰されたらtrue(ダメージを与える)
     */
    public boolean pushOutOfBlocks(Ship ship, double radius, java.awt.geom.Rectangle2D bounds) {
        if (blocks.isEmpty())
            return false;
        boolean crushed = false;
        Vector2 p = ship.getPos();
        for (BeamBlock b : blocks) {
            BeamBlock.Push result = b.pushOut(p, radius, bounds);
            if (result == BeamBlock.Push.SQUEEZED) {
                if (b.beginCrush())
                    crushed = true;
            } else {
                b.endCrush();
            }
        }
        return crushed;
    }

    /**
     * ブロックからビームを撃つ
     * 
     * @param block 撃つブロック(ビームは位置に追従する)
     * @param spec  ビーム定義
     * @param mode  ビームの色
     * @param dir   方向の単位ベクトル(dx, dy)
     */
    public void fireFromBlock(BeamBlock block, BeamSpec spec, ColorMode mode, double[] dir) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double offset = (spec.getWidth() * 0.0 + 18) * adj; // ブロックの縁から出す
        Supplier<Vector2> follow = () -> new Vector2(block.getPos().getX() + dir[0] * offset,
                block.getPos().getY() + dir[1] * offset);
        Beam beam = new Beam(Team.HOSTILE, spec, mode, follow.get(), Math.atan2(dir[1], dir[0]));
        beam.setOriginSupplier(follow);
        beams.add(beam);
    }

    /**
     * 撃つ位置の真上に敵がいるか(自機のビームは、狙える敵がいるときだけ撃つ)
     * 
     * @param origin    発射位置
     * @param halfWidth 左右にどれだけずれた敵まで狙うか(画面倍率込み)
     * @return 狙える敵がいればtrue
     */
    public boolean hasTargetAhead(Vector2 origin, double halfWidth) {
        List<Ship> targets = new ArrayList<>();
        targets.addAll(SHIP_M.getEnemyList());
        targets.addAll(SHIP_M.getBossList());
        for (Ship s : targets) {
            if (s.inScreen() && s.getPos().getY() < origin.getY()
                    && Math.abs(s.getPos().getX() - origin.getX()) <= halfWidth)
                return true;
        }
        return false;
    }

    /**
     * 敵・ボス・ギミックのビームを撃つ
     * 
     * @param spec   ビーム定義
     * @param source 撃つ船(ギミックならnull)
     * @param mode   ビームの色
     */
    public void fireHostile(BeamSpec spec, Ship source, ColorMode mode) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double w = MAIN.getWidth();
        double h = MAIN.getHeight();
        Player player = SHIP_M.getPlayer();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        Vector2 o;
        double deg;
        boolean trackX = false;
        boolean aim = false;
        Supplier<Vector2> follow = null;

        switch (spec.getOrigin()) {
            case TOP_PLAYER -> {
                o = new Vector2(player != null ? player.getPos().getX() : w / 2, -20 * adj);
                deg = 90;
                trackX = true;
            }
            case TOP_SELF -> {
                double sx = source != null ? source.getPos().getX() : w / 2;
                o = new Vector2(sx + spec.getX() * adj, -20 * adj);
                deg = 90;
            }
            case TOP_RANDOM -> {
                o = new Vector2(rnd.nextDouble(w * 0.08, w * 0.92), -20 * adj);
                deg = 90;
            }
            case TOP_X -> {
                o = new Vector2(w / 2 + spec.getX() * adj, -20 * adj);
                deg = 90;
            }
            case WALL_LEFT -> {
                o = new Vector2(-20 * adj, wallY(spec, h, rnd));
                deg = 0;
            }
            case WALL_RIGHT -> {
                o = new Vector2(w + 20 * adj, wallY(spec, h, rnd));
                deg = 180;
            }
            case WALL_RANDOM -> {
                boolean left = rnd.nextBoolean();
                o = new Vector2(left ? -20 * adj : w + 20 * adj, wallY(spec, h, rnd));
                deg = left ? 0 : 180;
            }
            case SHIP_AIM -> {
                Ship s = source;
                o = new Vector2(s != null ? s.getPos() : new Vector2(w / 2, 0));
                if (s != null)
                    follow = s::getPos;
                deg = 90;
                aim = true;
            }
            default -> { // SHIP_UP
                Ship s = source;
                o = new Vector2(s != null ? s.getPos() : new Vector2(w / 2, h / 2));
                if (s != null)
                    follow = s::getPos;
                deg = -90;
            }
        }

        Beam beam = new Beam(Team.HOSTILE, spec, mode, o, Math.toRadians(deg + spec.getAngle()));
        beam.setTrackPlayerX(trackX);
        beam.setTrackPlayerAim(aim);
        beam.setOriginSupplier(follow);
        beams.add(beam);
    }

    private double wallY(BeamSpec spec, double h, ThreadLocalRandom rnd) {
        // yの指定がなければ、自機がいる下寄りの範囲でランダム
        if (spec.getY() > 0)
            return spec.getY() * ShootingGameMain.getScreenSizeAdj();
        return rnd.nextDouble(h * 0.3, h * 0.9);
    }

    /**
     * 自機・サブ機のビームを撃つ(真上に撃つ)
     * 
     * @param spec           ビーム定義
     * @param originSupplier 発射位置(撃った機体に追従)
     * @param modeSupplier   色(撃った機体の今の色に追従)
     * @param damageScale    ダメージ倍率
     */
    public void firePlayer(BeamSpec spec, Supplier<Vector2> originSupplier, Supplier<ColorMode> modeSupplier,
            double damageScale) {
        Vector2 o = originSupplier.get();
        Beam beam = new Beam(Team.PLAYER, spec, modeSupplier.get(), o, Math.toRadians(-90 + spec.getAngle()));
        beam.setOriginSupplier(originSupplier);
        beam.setModeSupplier(modeSupplier);
        beam.setLengthToTop(true);
        beam.setDamageScale(damageScale);
        beams.add(beam);
    }

    @Override
    public void update(double dt) {
        Player player = SHIP_M.getPlayer();
        boolean playing = player != null && "none".equals(player.getAnimationName());

        // ステージのギミック(カウントダウン中などは進めない)
        if (playing) {
            for (BeamEmitter e : gimmicks)
                if (e.update(dt, 1.0))
                    fireHostile(e.getSpec(), null, e.pickColor(player.getMode()));
        }

        // ビームブロック(流れて画面から出たら消す)
        if (playing) {
            for (BeamBlock b : new ArrayList<>(blocks))
                b.update(dt, this);
            blocks.removeIf(BeamBlock::isGone);
        }

        tickTimer += dt;
        boolean tick = tickTimer >= TICK;
        if (tick)
            tickTimer -= TICK;

        for (Beam beam : new ArrayList<>(beams)) {
            beam.update(dt);
            if (beam.getState() != State.ACTIVE)
                continue;

            if (beam.getTeam() == Team.HOSTILE) {
                // 自機と同じ色のビームは当たらない(弾と同じルール)
                if (playing && beam.getMode() != player.getMode()
                        && beam.hitsCircle(player.getPos(), player.getCollisionSize()))
                    player.addDamage(DamageCalculator.toPlayer(beam.getDamage()));
            } else if (tick) {
                damageEnemies(beam);
            }
        }

        beams.removeIf(Beam::isDead);
    }

    /**
     * 自機のビームが敵にダメージを与える(貫通して、線上の全員に当たる)
     */
    private void damageEnemies(Beam beam) {
        List<Ship> targets = new ArrayList<>();
        targets.addAll(SHIP_M.getEnemyList());
        targets.addAll(SHIP_M.getBossList());

        for (Ship s : targets) {
            if (!s.inScreen() || !beam.hitsShip(s))
                continue;

            s.addDamage(DamageCalculator.beamToEnemy(
                    beam.getDamage() * beam.getDamageScale(), TICK, beam.getMode(), beam.getBossRate(), s));
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (BeamBlock b : new ArrayList<>(blocks))
            b.draw(g2);
        for (Beam beam : new ArrayList<>(beams))
            beam.draw(g2);
    }
}
