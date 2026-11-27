package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.factory.*;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BombProp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Android-free rules for all three modes. Only the game thread mutates it. */
public final class GameWorld {
    public final HeroAircraft hero = new HeroAircraft();
    private final List<AbstractEnemy> enemies = new ArrayList<>();
    private final List<BaseBullet> heroBullets = new ArrayList<>(), enemyBullets = new ArrayList<>();
    private final List<AbstractProp> props = new ArrayList<>();
    private final List<GameEvent> events = new ArrayList<>();
    private final Random random;
    private final Difficulty difficulty;
    private int scoreSinceLastBoss, bossSpawnCount;
    private BossEnemy boss;
    private int spawnCounter, score, ticks, backgroundOffset;
    private boolean gameOver;
    private long dragGesture = -1;
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    public GameWorld() { this(Difficulty.EASY); }
    public GameWorld(Random random) { this(Difficulty.EASY, random); }
    public GameWorld(Difficulty difficulty) { this(difficulty, new Random()); }
    public GameWorld(Difficulty difficulty, Random random) {
        this.difficulty = difficulty == null ? Difficulty.EASY : difficulty;
        this.random = random;
        hero.shootCycle = this.difficulty.heroShootTicks;
    }
    public Difficulty difficulty() { return difficulty; }
    public int difficultyTier() { return difficulty.tier(ticks); }
    public boolean isBossActive() { return boss != null && !boss.notValid(); }
    public BossEnemy activeBoss() { return isBossActive() ? boss : null; }
    public int score() { return score; }
    public int ticks() { return ticks; }
    public int backgroundOffset() { return backgroundOffset; }
    public boolean isGameOver() { return gameOver; }
    public List<AbstractEnemy> enemies() { return Collections.unmodifiableList(enemies); }
    public List<BaseBullet> heroBullets() { return Collections.unmodifiableList(heroBullets); }
    public List<BaseBullet> enemyBullets() { return Collections.unmodifiableList(enemyBullets); }
    public List<AbstractProp> props() { return Collections.unmodifiableList(props); }
    public List<GameEvent> drainEvents() {
        if (events.isEmpty()) return Collections.emptyList();
        List<GameEvent> batch = Collections.unmodifiableList(new ArrayList<>(events));
        events.clear();
        return batch;
    }

    public void tick(TouchState input, Viewport viewport) {
        if (gameOver) return;
        int tier = difficultyTier();
        hero.shootCycle = difficulty.heroShootTicks(tier);
        applyTouch(input, viewport);
        hero.tickEffects();
        for (AbstractEnemy enemy : enemies) enemy.tickEffects();
        for (BaseBullet bullet : enemyBullets) bullet.tickEffects();
        if (++spawnCounter >= difficulty.spawnTicks(tier)) {
            spawnCounter = 0;
            if (enemies.size() < difficulty.enemyLimit) {
                if (difficulty.bossThreshold > 0 && !isBossActive()
                        && scoreSinceLastBoss >= difficulty.bossThreshold) {
                    int hp = 3000 + (difficulty == Difficulty.HARD ? bossSpawnCount * 300 : 0);
                    boss = (BossEnemy) new BossFactory(hp).createEnemy();
                    placeEnemy(boss);
                    enemies.add(boss);
                    bossSpawnCount++;
                    scoreSinceLastBoss = 0;
                } else enemies.add(createEnemy());
            }
        }
        heroBullets.addAll(hero.executeShoot());
        for (AbstractEnemy enemy : enemies) {
            enemy.shootCycle = difficulty.enemyShootTicks(tier);
            enemyBullets.addAll(enemy.executeShoot());
        }
        for (BaseBullet bullet : heroBullets) bullet.forward();
        for (BaseBullet bullet : enemyBullets) bullet.forward();
        for (AbstractEnemy enemy : enemies) enemy.forward();
        for (AbstractProp prop : props) prop.forward();
        resolveCollisions();
        if (boss != null && boss.notValid()) boss = null;
        enemies.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
        backgroundOffset = (backgroundOffset + 2) % GameConfig.WINDOW_HEIGHT;
        ticks++;
        gameOver = hero.getHp() <= 0;
        if (gameOver) events.add(GameEvent.GAME_OVER);
    }

    private AbstractEnemy createEnemy() {
        double roll = random.nextDouble();
        EnemyFactory factory = roll < difficulty.mobCutoff ? new MobFactory()
                : roll < difficulty.eliteCutoff ? new EliteFactory()
                : roll < difficulty.elitePlusCutoff ? new ElitePlusFactory() : new EliteProFactory();
        AbstractEnemy enemy = factory.createEnemy();
        enemy.scaleSpawnSpeed(1 + difficultyTier());
        placeEnemy(enemy);
        return enemy;
    }

    private void placeEnemy(AbstractEnemy enemy) {
        // Center stays inside sprite-aware bounds; spawn randomness can be supplied by tests.
        int half = (enemy.getWidth() + 1) / 2;
        enemy.setLocation(half + random.nextInt(GameConfig.WINDOW_WIDTH - 2 * half + 1),
                random.nextInt((int) (GameConfig.WINDOW_HEIGHT * .05) + 1));
    }

    private void awardKill(AbstractEnemy enemy) {
        score += enemy.getScore();
        // Points earned while a live boss is present do not advance the next threshold.
        // Killing the boss itself counts toward the next interval, as in JavaFX.
        if (!isBossActive()) scoreSinceLastBoss += enemy.getScore();
    }

    private void applyTouch(TouchState input, Viewport view) {
        if (input == null || !input.active || view == null || input.viewportVersion != view.version) {
            dragging = false; dragGesture = -1; return;
        }
        if (input.gesture != dragGesture) {
            dragGesture = input.gesture;
            float x = view.worldX(input.downX), y = view.worldY(input.downY);
            dragging = Math.abs(x - hero.getLocationX()) <= hero.getWidth() / 2f
                    && Math.abs(y - hero.getLocationY()) <= hero.getHeight() / 2f;
            dragOffsetX = hero.getLocationX() - x; dragOffsetY = hero.getLocationY() - y;
        }
        if (dragging) {
            float x = view.worldX(input.x) + dragOffsetX;
            float y = view.worldY(input.y) + dragOffsetY;
            int halfW = hero.getWidth() / 2, halfH = (hero.getHeight() + 1) / 2;
            hero.setLocation(Math.max(halfW, Math.min(GameConfig.WINDOW_WIDTH - halfW, x)),
                    Math.max(halfH, Math.min(GameConfig.WINDOW_HEIGHT - halfH, y)));
        }
    }

    private void resolveCollisions() {
        for (BaseBullet bullet : enemyBullets) {
            if (hero.notValid()) break;
            if (!bullet.notValid() && hero.crash(bullet)) {
                hero.decreaseHp(bullet.getPower()); bullet.vanish();
                events.add(GameEvent.BULLET_HIT);
            }
        }
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) continue;
            for (AbstractEnemy enemy : enemies) {
                if (!enemy.notValid() && enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower()); bullet.vanish();
                    events.add(GameEvent.BULLET_HIT);
                    if (enemy.notValid()) { awardKill(enemy); props.addAll(enemy.generateProps()); }
                    break; // A normal bullet can hit only one target.
                }
            }
        }
        // Contact must be checked even when the hero bullet list is empty.
        for (AbstractEnemy enemy : enemies) {
            if (hero.notValid()) break;
            if (!enemy.notValid() && enemy.crash(hero)) {
                enemy.vanish(); hero.decreaseHp(Integer.MAX_VALUE);
            }
        }
        if (hero.notValid()) return;
        for (AbstractProp prop : props) {
            if (prop.notValid() || !hero.crash(prop)) continue;
            if (prop instanceof BombProp) {
                List<AbstractEnemy> alive = new ArrayList<>();
                for (AbstractEnemy enemy : enemies) if (!enemy.notValid()) alive.add(enemy);
                prop.effect(hero, enemyBullets, enemies);
                for (AbstractEnemy enemy : alive) if (enemy.notValid()) awardKill(enemy);
                events.add(GameEvent.BOMB_EXPLOSION);
            } else {
                prop.effect(hero, enemyBullets, enemies);
                events.add(GameEvent.GET_SUPPLY);
            }
            prop.vanish();
        }
    }
}
