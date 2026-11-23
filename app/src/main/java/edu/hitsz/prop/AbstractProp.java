package edu.hitsz.prop;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;

import java.util.List;

public abstract class AbstractProp extends AbstractFlyingObject {

    public AbstractProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override public void forward() {
        super.forward();
        if (locationY-getHeight()/2>edu.hitsz.application.GameConfig.WINDOW_HEIGHT) vanish();
    }

    public abstract void effect(HeroAircraft hero, List<BaseBullet> bullets, List<AbstractEnemy> enemies);
}
