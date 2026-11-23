package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Stage 2 checkpoint; replaced by SurfaceView in stage 3. */
final class ResourcePreview extends View {
    private final ImageManager images = new ImageManager();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    ResourcePreview(Context context) { super(context); images.load(getResources()); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xff101b27);
        float scale = Math.min(getWidth() / 600f, getHeight() / 900f);
        canvas.save();
        canvas.translate((getWidth() - 600 * scale) / 2, (getHeight() - 900 * scale) / 2);
        canvas.scale(scale, scale);
        canvas.drawBitmap(images.background(), null, new RectF(0, 0, 600, 900), paint);
        canvas.drawBitmap(images.get(Sprite.HERO), null, new RectF(250, 775, 350, 858), paint);
        canvas.restore();
    }
}
