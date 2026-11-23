package edu.hitsz.application;

/** Original PNG sizes in logical pixels. No Android dependency in model dimensions. */
public enum Sprite {
    HERO(100, 83), MOB(75, 50), ELITE(105, 68), ELITE_PLUS(105, 68),
    ELITE_PRO(100, 83), BOSS(300, 200), HERO_BULLET(10, 25), ENEMY_BULLET(10, 18),
    BLOOD(33, 30), BOMB(34, 34), BULLET(35, 33), BULLET_PLUS(34, 34), FREEZE(35, 35);

    public final int width;
    public final int height;
    Sprite(int width, int height) { this.width = width; this.height = height; }

    public static Sprite forObject(Object object) {
        switch (object.getClass().getSimpleName()) {
            case "HeroAircraft": return HERO;
            case "MobEnemy": return MOB;
            case "EliteEnemy": return ELITE;
            case "ElitePlusEnemy": return ELITE_PLUS;
            case "EliteProEnemy": return ELITE_PRO;
            case "BossEnemy": return BOSS;
            case "HeroBullet": return HERO_BULLET;
            case "EnemyBullet": return ENEMY_BULLET;
            case "BloodProp": return BLOOD;
            case "BombProp": return BOMB;
            case "BulletProp": return BULLET;
            case "BulletPlusProp": return BULLET_PLUS;
            case "FreezeProp": return FREEZE;
            default: throw new IllegalArgumentException("Unknown sprite: " + object.getClass());
        }
    }
}
