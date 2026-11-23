package edu.hitsz.bullet;
import edu.hitsz.application.GameConfig;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.observer.Observer;

public abstract class BaseBullet extends AbstractFlyingObject implements Observer {
    private final int power;
    public BaseBullet(int x,int y,int speedX,int speedY,int power) { super(x,y,speedX,speedY); this.power=power; }
    @Override public void forward() {
        super.forward();
        if (locationX+getWidth()/2<0 || locationX-getWidth()/2>GameConfig.WINDOW_WIDTH
            || locationY+getHeight()/2<0 || locationY-getHeight()/2>GameConfig.WINDOW_HEIGHT) vanish();
    }
    public int getPower() { return power; }
    @Override public void update(int type) {
        if (!(this instanceof EnemyBullet) || notValid()) return;
        if (type==1) vanish(); else if (type==2) applyFreeze(5000,0);
    }
}
