package edu.hitsz.prop;
import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.strategy.ScatteredShoot;
import java.util.List;
public class BulletProp extends AbstractProp {
    public BulletProp(int x,int y,int speedX,int speedY) { super(x,y,speedX,speedY); }
    @Override public void effect(HeroAircraft hero,List<BaseBullet> bullets,List<AbstractEnemy> enemies) {
        hero.activateWeapon(new ScatteredShoot());
    }
}
