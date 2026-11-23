package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.*;
import edu.hitsz.factory.*;
import edu.hitsz.prop.*;
import edu.hitsz.strategy.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Real rule regressions, runnable on the JVM without Android stubs or Maven dependencies. */
public final class GameWorldRegressionTest {
    private static int passed;
    private static final Viewport VIEW = new Viewport(600, 900, 1);
    @SuppressWarnings("unchecked")
    private static <T> List<T> list(GameWorld world, String name) throws Exception {
        Field field = GameWorld.class.getDeclaredField(name);
        field.setAccessible(true);
        return (List<T>) field.get(world);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void success(String name) { passed++; System.out.println("PASS " + name); }
    private static GameWorld world() { return new GameWorld(new Random(42)); }
    private static void tick(GameWorld world) { world.tick(TouchState.NONE, VIEW); }
    private static void tick(GameWorld world, int count) { for (int i = 0; i < count; i++) tick(world); }

    public static void main(String[] args) throws Exception {
        GameWorld world = world();
        tick(world, 5);
        check(world.heroBullets().isEmpty() && world.enemies().isEmpty(), "Nothing before its original cycle");
        tick(world);
        check(world.heroBullets().size() == 1 && world.heroBullets().get(0).getPower() == 30, "Hero shoots on tick 6");
        tick(world, 14);
        check(world.enemies().size() == 1, "First enemy on tick 20");
        check(!(world.enemies().get(0) instanceof BossEnemy), "No Easy-mode boss");
        success("original_easy_cycles");

        world = world();
        list(world, "enemies").add(new MobEnemy(200, 200, 0, 0, 30));
        list(world, "heroBullets").add(new HeroBullet(200, 200, 0, 0, 30));
        list(world, "heroBullets").add(new HeroBullet(200, 200, 0, 0, 30));
        tick(world); tick(world);
        check(world.score() == 10 && world.enemies().isEmpty(), "Two bullets never award duplicate kill score");
        success("single_kill_scored_once");

        world = world();
        list(world, "enemies").add(new MobEnemy(200, 200, 0, 0, 30));
        list(world, "enemies").add(new MobEnemy(200, 200, 0, 0, 30));
        list(world, "heroBullets").add(new HeroBullet(200, 200, 0, 0, 30));
        tick(world);
        check(world.score() == 10 && world.enemies().size() == 1, "One ordinary bullet cannot kill two overlapping enemies");
        success("bullet_consumed_on_first_hit");

        world = world();
        list(world, "enemies").add(new MobEnemy(300, 817, 0, 0, 30));
        tick(world);
        check(world.isGameOver() && world.heroBullets().isEmpty(), "Body contact works without hero bullets");
        int stoppedTicks = world.ticks();
        tick(world, 20);
        check(world.ticks() == stoppedTicks && world.score() == 0, "Game over stops simulation");
        success("contact_independent_of_bullets_and_game_over");

        world = world();
        list(world, "enemyBullets").add(new EnemyBullet(300, 817, 0, 0, 30));
        tick(world); tick(world);
        check(world.hero.getHp() == 270 && world.enemyBullets().isEmpty(), "Enemy bullet damages exactly once");
        success("enemy_bullet_single_damage");

        world = world();
        list(world, "enemies").add(new MobEnemy(200, 1000, 0, 0, 30));
        list(world, "heroBullets").add(new HeroBullet(-100, 100, 0, 0, 30));
        list(world, "enemyBullets").add(new EnemyBullet(200, 1000, 0, 0, 30));
        list(world, "props").add(new BloodProp(100, 1000, 0, 0));
        tick(world);
        check(world.enemies().isEmpty() && world.heroBullets().isEmpty()
                && world.enemyBullets().isEmpty() && world.props().isEmpty() && world.score() == 0, "Out-of-bounds objects cleaned without points");
        success("out_of_bounds_cleanup");

        HeroAircraft hero = new HeroAircraft(); hero.setLocation(100, 100);
        MobEnemy edge = new MobEnemy(187, 100, 0, 0, 30);
        check(!hero.crash(edge), "Original strict horizontal edge contact is not a hit");
        edge.setLocation(186, 100); check(hero.crash(edge), "One pixel overlap hits");
        edge.setLocation(100, 133); check(!hero.crash(edge), "Original reduced aircraft height retained");
        edge.setLocation(100, 132); check(hero.crash(edge), "Inside reduced height hits");
        success("original_collision_geometry");

        world = world();
        world.tick(new TouchState(true, 1, 1, 300, 817, -1000, 2000), VIEW);
        check(world.hero.getLocationX() == 50 && world.hero.getLocationY() == 858, "Hero fully inside bottom-left");
        world.tick(new TouchState(true, 1, 1, 300, 817, 2000, -1000), VIEW);
        check(world.hero.getLocationX() == 550 && world.hero.getLocationY() == 42, "Hero fully inside top-right");
        tick(world);
        world.tick(new TouchState(true, 2, 1, 10, 10, 300, 400), VIEW);
        check(world.hero.getLocationX() == 550, "A gesture outside hero does not control it");
        success("drag_clamp_and_reject_outside_start");

        world = world();
        world.tick(new TouchState(true, 1, 0, 300, 817, 500, 400), VIEW);
        check(world.hero.getLocationX() == 300, "Stale viewport version rejected");
        Viewport wide = new Viewport(1600, 900, 2);
        check(!wide.contains(0, 450), "Letterbox excluded");
        world.tick(new TouchState(true, 2, 2, 810, 817, 1010, 500), wide);
        check(world.hero.getLocationX() == 500 && world.hero.getLocationY() == 500, "Scale and touch offset agree");
        for (int[] size : new int[][]{{600,900},{1080,2400},{2400,1080},{720,1280}}) {
            Viewport view = new Viewport(size[0], size[1], 3);
            check(Math.abs(view.worldX(view.offsetX + 123 * view.scale) - 123) < .01, "X roundtrip");
            check(Math.abs(view.worldY(view.offsetY + 456 * view.scale) - 456) < .01, "Y roundtrip");
        }
        success("viewport_scaling_and_gesture_version");

        hero = new HeroAircraft();
        hero.activateWeapon(new ScatteredShoot());
        for (int i = 0; i < 200; i++) hero.tickEffects();
        hero.activateWeapon(new CircularShoot());
        for (int i = 0; i < 249; i++) hero.tickEffects();
        check(hero.shoot().size() == 20, "New power-up refreshes full 10-second duration");
        hero.tickEffects();
        check(hero.shoot().size() == 1, "Power-up expires at 10 seconds of game time");
        success("powerup_refresh_and_expiry");

        EliteEnemy elite = new EliteEnemy(200, 200, 0, 5, 30);
        elite.update(2);
        for (int i = 0; i < 99; i++) elite.tickEffects();
        check(elite.isFrozen() && elite.getSpeedY() == 0, "Freeze lasts 4 seconds");
        elite.tickEffects();
        check(!elite.isFrozen() && elite.getSpeedY() == 5, "Freeze restores velocity");
        MobEnemy mob = new MobEnemy(200, 200, 0, 5, 30); mob.update(2);
        for (int i = 0; i < 1000; i++) mob.tickEffects();
        check(mob.isFrozen() && mob.getSpeedY() == 0, "Permanent freeze does not overflow time");
        EnemyBullet bullet = new EnemyBullet(100, 100, 2, 6, 30); bullet.update(2);
        for (int i = 0; i < 250; i++) bullet.tickEffects();
        check(bullet.getSpeedY() == 6 && bullet.getSpeedX() == 2, "Enemy bullet restores after 5 seconds");
        success("freeze_duration_and_permanent_case");

        world = world();
        list(world, "enemies").add(new MobEnemy(100, 100, 0, 0, 30));
        EliteProEnemy survivor = new EliteProEnemy(200, 100, 0, 0, 100);
        list(world, "enemies").add(survivor);
        list(world, "props").add(new BombProp(300, 817, 0, 0));
        list(world, "enemyBullets").add(new EnemyBullet(100, 200, 0, 0, 30));
        tick(world);
        check(world.score() == 10 && survivor.getHp() == 70 && world.enemyBullets().isEmpty(), "Bomb awards only actually destroyed enemies");
        success("bomb_score_tracks_actual_deaths");

        hero = new HeroAircraft(); hero.decreaseHp(10);
        new BloodProp(0, 0, 0, 0).effect(hero, new ArrayList<>(), new ArrayList<>());
        check(hero.getHp() == 300, "Healing capped at maximum");
        check(new HeroAircraft().getHp() == 300, "Worlds do not share singleton state");
        success("healing_and_world_isolation");

        EnemyFactory[] factories = {new MobFactory(),new EliteFactory(),new ElitePlusFactory(),new EliteProFactory(),new BossFactory(1200)};
        for (EnemyFactory factory : factories) for (int i = 0; i < 100; i++) {
            AbstractEnemy enemy = factory.createEnemy();
            check(enemy.getLocationX() - enemy.getWidth() / 2f >= 0
                    && enemy.getLocationX() + enemy.getWidth() / 2f <= 600, "Spawn center within bounds");
        }
        success("factory_spawn_bounds");

        world = world();
        for (int i = 0; i < 7500; i++) {
            // Observe a full five minutes while keeping the test pilot alive.
            world.hero.increaseHp(300);
            for (AbstractEnemy enemy : world.enemies()) if (enemy.getLocationY() > 700) enemy.vanish();
            tick(world);
            // Ring power-up fires 20 bullets every six ticks. Its valid working set exceeds 100.
            check(world.enemies().size() <= 5 && world.heroBullets().size() < 1000
                    && world.enemyBullets().size() < 300 && world.props().size() < 100,
                    "Collections at tick " + i + ": enemies=" + world.enemies().size()
                            + " heroBullets=" + world.heroBullets().size() + " enemyBullets=" + world.enemyBullets().size()
                            + " props=" + world.props().size());
            check(!world.isGameOver(), "Test pilot alive");
        }
        check(world.ticks() == 7500, "Five simulated minutes completed");
        success("five_minutes_simulated_cleanup");

        hero = new HeroAircraft(); hero.activateWeapon(new CircularShoot());
        List<BaseBullet> ring = hero.shoot();
        for (int i = 0; i < 400; i++) for (BaseBullet shot : ring) if (!shot.notValid()) shot.forward();
        check(ring.stream().allMatch(BaseBullet::notValid), "Every ring bullet eventually leaves the world");
        success("all_ring_directions_eventually_cleaned");
        System.out.println("RESULT " + passed + "/" + passed + " regression groups passed");
    }
}
