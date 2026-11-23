package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.application.Sprite;
import edu.hitsz.application.GameConfig;

// 普通机工厂
public class ElitePlusFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy() {
        return new ElitePlusEnemy(
                (int) (Math.random() * (GameConfig.WINDOW_WIDTH - Sprite.ELITE_PLUS.width)) + (Sprite.ELITE_PLUS.width + 1) / 2,
                (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                2, // speedX
                5, // speedY
                30 // hp
        );
    }
}
