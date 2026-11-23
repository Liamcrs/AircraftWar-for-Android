package edu.hitsz.aircraft;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.Sprite;
import edu.hitsz.strategy.ShootStrategy;
import edu.hitsz.strategy.StraightShoot;

/** Per-world hero replaces the static singleton, isolating recreated Activities. */
public final class HeroAircraft extends AbstractAircraft {
    private int weaponRemaining;
    public HeroAircraft() {
        super(GameConfig.WINDOW_WIDTH/2,GameConfig.WINDOW_HEIGHT-Sprite.HERO.height,0,0,300);
        power=30; direction=-1; shootNum=1; shootCycle=6;
        setShootStrategy(new StraightShoot());
    }
    public void increaseHp(int amount) { if (!notValid()) hp=Math.min(hp+amount,maxHp); }
    @Override public void forward() { /* Touch-controlled. */ }
    public void activateWeapon(ShootStrategy strategy) { setShootStrategy(strategy); weaponRemaining=10000; }
    @Override public void tickEffects() {
        super.tickEffects();
        if (weaponRemaining>0) {
            weaponRemaining=Math.max(0,weaponRemaining-GameConfig.TICK_MS);
            if (weaponRemaining==0) setShootStrategy(new StraightShoot());
        }
    }
}
