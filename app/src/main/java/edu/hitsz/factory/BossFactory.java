package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.application.Sprite;
import edu.hitsz.application.GameConfig;

// 普通机工厂
public class BossFactory implements EnemyFactory {
    private int hp;

    public BossFactory(int hp){
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy() {
        return new BossEnemy(
                (int) (Math.random() * (GameConfig.WINDOW_WIDTH - Sprite.BOSS.width)) + (Sprite.BOSS.width + 1) / 2,
                (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                -2, // speedX
                0, // speedY
                hp // hp
        );
    }
}