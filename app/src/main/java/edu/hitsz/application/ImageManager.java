package edu.hitsz.application;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import edu.hitsz.aircraftwar.R;
import java.util.EnumMap;
import java.util.Map;

/** Decode once per View instance; never keep an Activity or decode in the game loop. */
public final class ImageManager {
    private final Map<Sprite, Bitmap> images = new EnumMap<>(Sprite.class);
    private Bitmap background;

    public void load(Resources resources) {
        if (background != null) return;
        put(resources, Sprite.HERO, R.drawable.hero);
        put(resources, Sprite.MOB, R.drawable.mob);
        put(resources, Sprite.ELITE, R.drawable.elite);
        put(resources, Sprite.ELITE_PLUS, R.drawable.elite_plus);
        put(resources, Sprite.ELITE_PRO, R.drawable.elite_pro);
        put(resources, Sprite.BOSS, R.drawable.boss);
        put(resources, Sprite.HERO_BULLET, R.drawable.bullet_hero);
        put(resources, Sprite.ENEMY_BULLET, R.drawable.bullet_enemy);
        put(resources, Sprite.BLOOD, R.drawable.prop_blood);
        put(resources, Sprite.BOMB, R.drawable.prop_bomb);
        put(resources, Sprite.BULLET, R.drawable.prop_bullet);
        put(resources, Sprite.BULLET_PLUS, R.drawable.prop_bullet_plus);
        put(resources, Sprite.FREEZE, R.drawable.prop_freeze);
        background = decode(resources, R.drawable.bg_easy);
    }

    private void put(Resources resources, Sprite sprite, int id) {
        Bitmap bitmap = decode(resources, id);
        if (bitmap.getWidth() != sprite.width || bitmap.getHeight() != sprite.height) {
            throw new IllegalStateException("Sprite dimensions disagree: " + sprite);
        }
        images.put(sprite, bitmap);
    }

    private static Bitmap decode(Resources resources, int id) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, id, options);
        if (bitmap == null) throw new IllegalStateException("Cannot decode resource " + id);
        return bitmap;
    }

    public Bitmap background() { return background; }
    public Bitmap get(Sprite sprite) { return images.get(sprite); }
}
