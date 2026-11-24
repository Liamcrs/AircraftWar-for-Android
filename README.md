# AircraftWar-for-Android

将 JavaFX 飞机大战迁移到 Android 的原生 Java 项目，采用 `Activity + SurfaceView + Runnable`，最低支持 Android 11 / API 30。

仓库：[Liamcrs/AircraftWar-for-Android](https://github.com/Liamcrs/AircraftWar-for-Android)

## 游戏功能

- 默认进入简单模式，背景滚动，英雄机自动发射子弹。
- 按住英雄机拖动，机体保持在游戏区域内。
- 普通、精英、精锐和王牌敌机，以及敌方子弹。
- 碰撞检测、生命值、击毁计分和出界清理。
- 回血、炸弹、散射增强、环射增强和冻结道具。
- 原版红色HUD、结束画面和冰冻效果；结束后点击画面开始新一局。
- 前后台切换、Surface重建与游戏线程启停管理。

游戏使用600×900逻辑画布和40ms逻辑步长，画面等比适配不同屏幕。

## 使用 Android Studio

1. 克隆仓库并打开工程根目录 `AircraftWar-for-Android`。
2. 配置 Android SDK，并将 Gradle JDK 设为 Android Studio 自带的 JBR。
3. 完成 Gradle 同步，启动模拟器或连接开启 USB 调试的设备。
4. 选择 `app` 模块并点击 Run。

构建配置：

| 配置 | 版本 |
| --- | --- |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Compile / Target SDK | 37 |
| Minimum SDK | 30 |
| SDK Build Tools | 36.0.0 |
| Java源码兼容级别 | 11 |

标准命令行构建：

```bash
./gradlew :app:assembleDebug
```

## 使用已安装 SDK 离线构建

也可以直接使用本机的 aapt2、javac、D8、zipalign 和 apksigner 打包：

```bash
python3 scripts/build_offline.py
```

脚本默认读取 macOS 的 Android Studio 内置 JBR 和 `~/Library/Android/sdk`；可以通过 `JAVA_HOME`、`ANDROID_HOME` 指定其他位置。需要安装 `platforms/android-37.0` 与 `build-tools/36.0.0`。

生成的APK位于：

```text
app/build/offline/debug/app-debug.apk
```

## 源码结构

Java源码位于 `app/src/main/java/edu/hitsz/`：

| 包 | 职责 |
| --- | --- |
| application | Activity入口、SurfaceView循环、Canvas绘制、游戏世界、图片缓存与坐标变换 |
| basic | 飞行物基础属性、移动和碰撞 |
| aircraft | 英雄机与敌机继承体系 |
| bullet | 双方子弹、伤害与出界处理 |
| factory | 敌机及道具创建 |
| strategy | 直射、散射和环射策略 |
| prop | 道具效果与局内计时 |
| observer | 敌机和子弹的效果通知接口 |

图片位于 `app/src/main/res/drawable/`，窗口主题位于 `app/src/main/res/values/styles.xml`。

界面入口为 `MainActivity`；`BaseGame` 管理Surface与游戏线程；`EasyGame`负责画面；`GameWorld`处理独立于Android的游戏逻辑。
