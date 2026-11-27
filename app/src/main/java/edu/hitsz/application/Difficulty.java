package edu.hitsz.application;

/** Mode parameters are independent of Android and use effective simulation ticks. */
public enum Difficulty {
    EASY("Easy", 5, 20, 6, 10, .70, .90, .95, 0),
    NORMAL("Normal", 8, 15, 6, 12, .50, .75, .90, 1000),
    HARD("Hard", 10, 10, 8, 10, .30, .55, .80, 800);

    public final String hudName;
    public final int enemyLimit, spawnTicks, heroShootTicks, enemyShootTicks, bossThreshold;
    final double mobCutoff, eliteCutoff, elitePlusCutoff;

    Difficulty(String hudName, int enemyLimit, int spawnTicks, int heroShootTicks,
               int enemyShootTicks, double mobCutoff, double eliteCutoff,
               double elitePlusCutoff, int bossThreshold) {
        this.hudName = hudName;
        this.enemyLimit = enemyLimit;
        this.spawnTicks = spawnTicks;
        this.heroShootTicks = heroShootTicks;
        this.enemyShootTicks = enemyShootTicks;
        this.mobCutoff = mobCutoff;
        this.eliteCutoff = eliteCutoff;
        this.elitePlusCutoff = elitePlusCutoff;
        this.bossThreshold = bossThreshold;
    }

    public static Difficulty fromName(String value) {
        for (Difficulty mode : values()) if (mode.name().equals(value)) return mode;
        return EASY;
    }

    public int tier(int ticks) {
        return this == EASY ? 0 : ticks / (30_000 / GameConfig.TICK_MS);
    }
    public int spawnTicks(int tier) {
        return this == EASY ? spawnTicks : Math.max(this == HARD ? 4 : 5, spawnTicks - tier * 2);
    }
    public int heroShootTicks(int tier) {
        return this == HARD ? Math.max(4, heroShootTicks - tier) : heroShootTicks;
    }
    public int enemyShootTicks(int tier) {
        return this == HARD ? Math.max(5, enemyShootTicks - tier) : enemyShootTicks;
    }
}
