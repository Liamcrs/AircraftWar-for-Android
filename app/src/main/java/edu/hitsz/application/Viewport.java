package edu.hitsz.application;

/** Immutable shared coordinate transform. Surface dimensions already exclude system insets. */
public final class Viewport {
    public final float scale, offsetX, offsetY;
    public final int version;
    public Viewport(int width, int height, int version) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Empty viewport");
        scale = Math.min(width / (float) GameConfig.WINDOW_WIDTH, height / (float) GameConfig.WINDOW_HEIGHT);
        offsetX = (width - GameConfig.WINDOW_WIDTH * scale) / 2;
        offsetY = (height - GameConfig.WINDOW_HEIGHT * scale) / 2;
        this.version = version;
    }
    public float worldX(float screenX) { return (screenX - offsetX) / scale; }
    public float worldY(float screenY) { return (screenY - offsetY) / scale; }
    public boolean contains(float x, float y) {
        return worldX(x) >= 0 && worldX(x) <= GameConfig.WINDOW_WIDTH
                && worldY(y) >= 0 && worldY(y) <= GameConfig.WINDOW_HEIGHT;
    }
}
