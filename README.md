# AircraftWar2026 · 软件构造实践实验二

将本人原有 JavaFX 飞机大战迁移为 Android 原生 Java 游戏。默认进入简单模式，采用 `Activity + SurfaceView + Runnable`，最低支持 Android 11 / API 30。

## 已实现

- 原版背景、英雄机、四类普通敌机、双方子弹的 Bitmap 绘制与背景滚动。
- 按住英雄机拖动，自动射击；机体完整限制在游戏区域内；支持触摸取消。
- 原版40ms逻辑步长、600×900逻辑画布、每6 tick英雄射击、每20 tick生成敌机、最多5架敌机。
- 原版伤害、敌机得分、工厂模式与直射/散射/环射策略；保留回血、炸弹、火力和冻结道具。
- 修正重复命中/计分、机体碰撞依赖子弹循环、图片密度缩放、出界清理和跨线程道具计时问题。
- 局内HUD、结束画面配色/位置和冰冻特效按原版JavaFX恢复；保留实验二默认简单模式及移动端重新开始提示。
- 暂停/恢复、Surface重建、退出线程管理；结束后在画布内点击开启新一局。
- 16组可独立运行的逻辑回归测试；真实设备启动、拖动、取消、发射、刷怪、五次前后台切换与退出测试入口。

本次未加入实验三以后的难度页面、音效、排行榜和网络对战。

## 用 Android Studio 运行

1. 打开本目录 `AircraftWar2026`，不要只打开 `app` 目录。
2. Gradle JDK 选择 Android Studio 自带的 JBR；工程使用 AGP 9.4.0、Gradle 9.6.0、SDK 37、Build Tools 36.0.0。
3. Gradle 同步需要能访问 Google Maven 与 Gradle 下载服务。应用本身不依赖第三方库。
4. 在 Device Manager 启动已有模拟器，或连接已开启 USB 调试的真机，选择 `app` 并点击 Run。
5. 游戏中按住英雄机拖动。射击自动进行，死亡后点击画面重新开始。

本机安装的 Android Studio 显示版本为2026.2，课程指导书写的是 Panda。本次使用既有 SDK/JBR，没有替换现有 IDE；若课程严格指定 IDE 版本，应另外安装课程版本，勿覆盖原有配置。

## 离线构建与测试

为避免首次同步受网络影响，提供直接调用已安装 Android SDK 工具的构建脚本。它依次执行 aapt2、javac、D8、zipalign、apksigner，产物为真实签名 APK，无需下载 Gradle 或 Maven 依赖。

```bash
python3 scripts/test_world.py
python3 scripts/build_offline.py
```

默认读取本机 Android Studio 内置 JBR 和 `~/Library/Android/sdk`。不同位置可设置 `JAVA_HOME` 与 `ANDROID_HOME`。本脚本当前固定使用已安装的 `platforms/android-37.0` 和 `build-tools/36.0.0`。

游戏 APK：`app/build/offline/debug/app-debug.apk`。

启动模拟器后执行真实设备验证：

```bash
python3 scripts/run_device_checks.py
```

也可加 `--start-emulator` 自动启动首个已有 AVD；多设备时用 `--serial` 选择。设备脚本先构建两个 APK，再安装、运行测试；通过后重新打开游戏。结果保存在 `app/build/device-checks.txt`。

只想安装并查看游戏界面，可以执行 `python3 scripts/run_device_checks.py --install-only`。此模式不执行设备验收，不能据此判定恢复测试通过。

标准 Gradle 构建入口保留为：

```bash
./gradlew :app:assembleDebug
```

## 当前验证状态

已验证：全部 Java 源码编译、资源打包、DEX 生成、APK 签名、Manifest最低API与启动入口、16组逻辑回归测试、五分钟逻辑模拟。2026年11月24日用户已在本机成功安装两个APK并确认游戏启动及基础功能可用。

设备测试首次执行在恢复环节报 `Resume failed`；前面的启动、拖动/取消、射击/生成断言已执行。当前修订版改用测试shell触发回前台、跟踪实际恢复的Activity，并通过成功提交的绘制帧验证循环，避免Game Over停止逻辑tick后的误判。修订版已重新编译和签名，仍需用户再次运行确认。此会话的ADB端口限制仍然存在；Android Studio Gradle同步及修订版UI尚未在设备上复验。

## 目录

| 路径 | 内容 |
| --- | --- |
| app/src/main/java/edu/hitsz/application | Activity、SurfaceView、Canvas渲染、独立逻辑世界、资源与坐标适配 |
| app/src/main/java/edu/hitsz/aircraft、bullet、factory、strategy、prop、observer | 从本人原版迁移的领域类 |
| app/src/main/res/drawable | 原版图片，全部使用合法 Android 资源名 |
| app/src/androidTest | 不依赖 AndroidX 的真实设备验证入口 |
| tests | 对实际模型执行的 JVM 回归测试 |
| scripts | 离线构建、模型测试、设备验证 |
| docs | 迁移记录、实验验收与AI使用日志 |

## Git 与课程提交

本地仓库按四阶段保存真实提交。查看：

```bash
git log --oneline
```

尚未设置本课程的 Gitee 远程地址，未向原版 GitHub 仓库写入任何内容。取得课程 Gitee 地址后，在本工程关联该地址并推送 `main`。结题所需的运行截图、录屏和提交日志截图，应在实际运行或打开提交记录后保存，不能用本次逻辑测试替代。

如果使用下载的交付包，`history/experiment2.bundle` 保存完整四阶段提交，可用 `git clone history/experiment2.bundle ../AircraftWar2026-with-history` 恢复带历史的源码目录。

原版来源与保留/修改项见 [迁移记录](docs/迁移记录.md)，实际通过项与未验证项见 [实验二验收记录](docs/实验二验收记录.md)。
