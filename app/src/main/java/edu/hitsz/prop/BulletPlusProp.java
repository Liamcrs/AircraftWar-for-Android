package edu.hitsz.prop;
import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.strategy.CircularShoot;
import java.util.List;
public class BulletPlusProp extends AbstractProp {
    public BulletPlusProp(int x,int y,int speedX,int speedY) { super(x,y,speedX,speedY); }
    @Override public void effect(HeroAircraft hero,List<BaseBullet> bullets,List<AbstractEnemy> enemies) {
        hero.activateWeapon(new CircularShoot());
    }
}
