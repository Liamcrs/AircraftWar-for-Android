package edu.hitsz.application;

/** Whole gesture snapshot; MOVE cannot overwrite the original DOWN anchor. */
public final class TouchState {
    public static final TouchState NONE = new TouchState(false, 0, 0, 0, 0, 0, 0);
    public final boolean active;
    public final long gesture;
    public final int viewportVersion;
    public final float downX, downY, x, y;
    public TouchState(boolean active, long gesture, int viewportVersion, float downX, float downY, float x, float y) {
        this.active = active; this.gesture = gesture; this.viewportVersion = viewportVersion;
        this.downX = downX; this.downY = downY; this.x = x; this.y = y;
    }
}
