package edu.hitsz.aircraft;
import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.Observer;
import edu.hitsz.prop.AbstractProp;
import java.util.List;

/** Original enemy hierarchy; effect expiry uses game time, without worker threads. */
public abstract class AbstractEnemy extends AbstractAircraft implements Observer {
    private boolean killedByBomb;
    public AbstractEnemy(int x,int y,int speedX,int speedY,int hp) { super(x,y,speedX,speedY,hp); }
    public abstract int getScore();
    public abstract List<AbstractProp> generateProps();
    public boolean isKilledByBomb() { return killedByBomb; }
    /** Called exactly once for a fresh enemy before any effects are applied. */
    public final void scaleSpawnSpeed(int multiplier) {
        if (isFrozen()) throw new IllegalStateException("Cannot scale a frozen enemy");
        speedX *= multiplier;
        speedY *= multiplier;
    }
    @Override public void forward() {
        super.forward();
        int half=(getWidth()+1)/2;
        if (locationX<half) { locationX=half; speedX=Math.abs(speedX); }
        if (locationX>GameConfig.WINDOW_WIDTH-half) { locationX=GameConfig.WINDOW_WIDTH-half; speedX=-Math.abs(speedX); }
        if (locationY-getHeight()/2>GameConfig.WINDOW_HEIGHT) vanish();
    }
    @Override public void update(int type) {
        if (notValid()) return;
        if (type==1) {
            if (this instanceof EliteProEnemy) decreaseHp(30);
            else if (!(this instanceof BossEnemy)) { killedByBomb=true; vanish(); }
        } else if (type==2 && !(this instanceof BossEnemy)) {
            if (this instanceof MobEnemy) applyFreeze(-1,0);
            else if (this instanceof ElitePlusEnemy) applyFreeze(3000,0);
            else if (this instanceof EliteProEnemy) applyFreeze(5000,2);
            else applyFreeze(4000,0);
        }
    }
}
