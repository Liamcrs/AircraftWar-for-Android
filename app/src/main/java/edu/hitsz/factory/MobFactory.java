package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.application.Sprite;
import edu.hitsz.application.GameConfig;

// 普通机工厂
public class MobFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy() {
        return new MobEnemy(
                (int) (Math.random() * (GameConfig.WINDOW_WIDTH - Sprite.MOB.width)) + (Sprite.MOB.width + 1) / 2,
                (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                0, // speedX
                5, // speedY
                30// hp
        );
    }
}
