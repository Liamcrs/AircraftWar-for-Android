package edu.hitsz.basic;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.Sprite;

/** Original center coordinates and collision algorithm, independent of the UI. */
public abstract class AbstractFlyingObject {
    protected int locationX, locationY, speedX, speedY;
    protected boolean isValid = true;
    private int freezeRemaining;
    private int originalSpeedX, originalSpeedY;
    protected AbstractFlyingObject(int x, int y, int speedX, int speedY) {
        locationX=x; locationY=y; this.speedX=speedX; this.speedY=speedY;
    }
    public void forward() { locationX+=speedX; locationY+=speedY; }
    public boolean crash(AbstractFlyingObject other) {
        int factor=this instanceof AbstractAircraft ? 2 : 1;
        int otherFactor=other instanceof AbstractAircraft ? 2 : 1;
        return other.locationX+(other.getWidth()+getWidth())/2 > locationX
            && other.locationX-(other.getWidth()+getWidth())/2 < locationX
            && other.locationY+(other.getHeight()/otherFactor+getHeight()/factor)/2 > locationY
            && other.locationY-(other.getHeight()/otherFactor+getHeight()/factor)/2 < locationY;
    }
    public int getLocationX() { return locationX; }
    public int getLocationY() { return locationY; }
    public int getSpeedY() { return speedY; }
    public int getSpeedX() { return speedX; }
    public void setLocation(double x,double y) { locationX=(int)x; locationY=(int)y; }
    public Sprite getSprite() { return Sprite.forObject(this); }
    public int getWidth() { return getSprite().width; }
    public int getHeight() { return getSprite().height; }
    public boolean notValid() { return !isValid; }
    public void vanish() { isValid=false; }
    public boolean isFrozen() { return freezeRemaining!=0; }
    /** Negative duration means permanent; refresh never halves velocity twice. */
    protected final void applyFreeze(int durationMs,int speedDivisor) {
        if (!isFrozen()) { originalSpeedX=speedX; originalSpeedY=speedY; }
        freezeRemaining=durationMs;
        speedX=speedDivisor==0 ? 0 : originalSpeedX/speedDivisor;
        speedY=speedDivisor==0 ? 0 : originalSpeedY/speedDivisor;
    }
    public void tickEffects() {
        if (freezeRemaining>0) {
            freezeRemaining=Math.max(0,freezeRemaining-GameConfig.TICK_MS);
            if (freezeRemaining==0) { speedX=originalSpeedX; speedY=originalSpeedY; }
        }
    }
}
