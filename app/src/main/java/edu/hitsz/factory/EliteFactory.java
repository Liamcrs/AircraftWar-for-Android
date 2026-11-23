package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.application.Sprite;
import edu.hitsz.application.GameConfig;

// 普通机工厂
public class EliteFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy() {
        return new EliteEnemy(
                (int) (Math.random() * (GameConfig.WINDOW_WIDTH - Sprite.ELITE.width)) + (Sprite.ELITE.width + 1) / 2,
                (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                0, // speedX
                5, // speedY
                30 // hp
        );
    }
}
