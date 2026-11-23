package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.EliteProEnemy;
import edu.hitsz.application.Sprite;
import edu.hitsz.application.GameConfig;

// 普通机工厂
public class EliteProFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy() {
        return new EliteProEnemy(
                (int) (Math.random() * (GameConfig.WINDOW_WIDTH - Sprite.ELITE_PRO.width)) + (Sprite.ELITE_PRO.width + 1) / 2,
                (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                -2, // speedX
                5, // speedY
                30 // hp
        );
    }
}
