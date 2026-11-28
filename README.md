# AircraftWar-for-Android

将 JavaFX 飞机大战迁移到 Android 的原生 Java 项目，采用 `Activity + SurfaceView + Runnable`，最低支持 Android 11 / API 30。

仓库：[Liamcrs/AircraftWar-for-Android](https://github.com/Liamcrs/AircraftWar-for-Android)

## 游戏功能

- 启动后进入主菜单，可选择简单、普通或困难模式；不同难度具有独立背景、敌机数量、生成与射击节奏。
- 普通和困难模式随有效游戏时间逐步提升难度，达到分数阈值出现 Boss；困难模式的后续 Boss 生命值增加。
- 主菜单的「音乐与音效」开关默认开启，选择自动保存，下次启动仍然有效；关闭时游戏音乐与音效均静音。
- 背景滚动，英雄机自动发射子弹。
- 按住英雄机拖动，机体保持在游戏区域内。
- 普通、精英、精锐和王牌敌机，以及敌方子弹。
- 碰撞检测、生命值、击毁计分和出界清理。
- 回血、炸弹、散射增强、环射增强和冻结道具。
- 原版红色HUD、Boss生命条、结束画面和冰冻效果；游戏结束后通过弹窗选择重新开始当前难度或返回菜单。
- 主菜单可打开排行榜；游戏结束后输入姓名保存本局成绩，按分数降序排列，第一名高亮显示。支持删除单条记录并重新排名。
- 排行榜使用 ListView、自定义 BaseAdapter 和 ViewHolder，通过 DAO 隔离界面与存储。最终使用应用私有 SQLite 数据库 `rank.db`，重启后保留记录；从文件存储版本升级时自动迁移 `rank.json`，迁移标记与导入记录在同一事务内提交。
- 普通背景音乐循环播放，Boss 存活期间切换为 Boss 音乐；击中、拾取道具、炸弹及游戏结束具有原版音效。
- 前后台切换、Surface重建、游戏线程启停及音频资源释放管理；后台暂停所有播放，恢复时仅继续当前局应播放的背景音乐。

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
| application | 菜单与游戏 Activity、难度配置、SurfaceView循环、Canvas绘制、游戏世界与事件、图片缓存和坐标变换 |
| music | MediaPlayer 循环音乐与 SoundPool 短音效、主线程播放状态和资源生命周期 |
| basic | 飞行物基础属性、移动和碰撞 |
| aircraft | 英雄机与敌机继承体系 |
| bullet | 双方子弹、伤害与出界处理 |
| factory | 敌机及道具创建 |
| strategy | 直射、散射和环射策略 |
| prop | 道具效果与局内计时 |
| observer | 敌机和子弹的效果通知接口 |
| rank | 成绩模型、排行榜 Adapter、DAO 接口、文件与 SQLite 存储实现、异步存储协调 |

图片位于 `app/src/main/res/drawable/`，原版 WAV 音频位于 `app/src/main/res/raw/`（打包时不压缩），窗口主题位于 `app/src/main/res/values/styles.xml`。

界面入口为 `MainActivity`，通过显式 Intent 将难度与音频设置传给 `GameActivity`；`BaseGame` 管理 Surface 与游戏线程，`GameScene` 共享绘制与局状态管理，`EasyGame`、`NormalGame`、`HardGame` 提供三种场景。`GameWorld` 处理独立于 Android 的游戏逻辑，并由游戏线程通过主线程 Handler 交付不可变事件批次。`GameAudioManager` 在主线程管理互斥背景音乐与音效，响应游戏结束、重新开始及 Activity 生命周期。

实验四按四次提交实现：排行榜页面 → 展示数据及文件持久化 → 删除记录 → SQLite 存储。`RankActivity` 和 `RankAdapter` 不直接访问文件或数据库，`RankRepository` 在单独线程中通过 `RankDao` 访问存储，再将结果交回主线程。成绩以每局唯一 ID 去重，同名玩家的多局记录独立保存；同分时按记录时间、ID 排序。
