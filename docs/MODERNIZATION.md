# 现代化迁移记录

方案与任务：[tv-modernization](../.scratch/tv-modernization/spec.md)。用户已授权按阶段执行；每阶段保持原生 UI 与已可用能力，Debug 验证通过后提交。

## P1：构建链与基线

- 固定原 APK a2f5494、工具 d1cf993，保留原始交付包不覆盖。
- AGP 8.6.1 / Gradle 8.7 / Kotlin 2.0.21 / Compose compiler 2.0.21 / Compose BOM 2024.10.01 / TV Material 1.0.1；compileSdk 35、minSdk 28、targetSdk 28，包名 com.coke.tv。
- Chaquopy 16.1.0，Python 保持 3.8，ARMv7 / ARM64；pip 产物版本固化在 pyramid/requirements.txt。
- 沿用 JDK 17；新增 SDK platform 35 与 build-tools 34.0.0。没有安装 NDK，现有库不需要重新编译 JNI。
- AGP 8 显式 namespace / BuildConfig；保留非 final、跨模块 R，避免破坏上游 switch / 资源引用。
- 只加入 Compose 主题与依赖，没有替换页面或播放器。

### 实际验证

`assemblePython32Debug`、`assemblePython64Debug`、`assemblePython64DebugAndroidTest` 成功。包身份、API 28、单 ABI、QuickJS / Python、无预置源与用户测试订阅校验通过。

自有动态 JAR 用 javac + D8 构建，只放入测试 APK；原 JarLoader 加载后执行 home / player 与 siteKey 契约。JsSpider 与 PythonLoader 通过测试进程内的 loopback HTTP 服务加载自有源，执行 home / player。Python 样本导入原 base.spider，实际加载 lxml 等依赖。ARM64 模拟器 3/3 通过，不代表 ARMv7 设备已执行。

原 APK 与候选分别捕获 6 页 × 2 尺寸 × 2 主题。排除顶部时钟后，21/24 组像素一致；直播 3 组差异只在加载图标的旋转位置（69 / 83 / 168 像素），非布局变化。原截图已纳入方案基线，候选、比较 JSON 与 APK 位于 ignored artifacts/modernization。

同一临时 API 33 Android TV 设备、5 次冷启动、启动后 5 秒 PSS：基线中位数 243 ms / 106696 KiB；P1 269 ms / 127089 KiB，分别约 +10.7% / +19.1%。这是 am start TotalTime 与 Debug 空首页进程 PSS，不是可操作首帧或 ARMv7 整机性能；不能外推实际电视性能。P3 渲染 Compose 后需再测。

### 复现命令

```sh
sh scripts/android-build.sh :app:assemblePython32Debug :app:assemblePython64Debug
sh scripts/build-engine-fixture.sh python64
sh scripts/android-build.sh :app:assemblePython64DebugAndroidTest
```

在专用测试设备安装 ARM64 包与 AndroidTest 包后，用 `am instrument -w -e class com.github.tvbox.osc.SourceEngineCompatibilityTest com.coke.tv.test/androidx.test.runner.AndroidJUnitRunner` 执行源样本；VisualBaselineTest 可通过 label 参数输出截图。测试 HTTP 服务只运行在测试进程内，After 停止。

P1 新建临时 AVD TV_Migration_Test，保留的 TV_1080p_API33 数据未改。Google Play 自动更新后对 targetSdk 28 调试包显示旧目标版本保护提示；在临时 AVD 完成调试安装，并关闭该临时设备的包验证和 Play Store，避免安装等待。此处不改变宿主或长期 AVD 设置；任务结束删除临时 AVD。targetSdk 升级与权限变化仍单独处理。

技术依据：[AGP 8.6 兼容表](https://developer.android.com/build/releases/agp-8-6-0-release-notes)、[Chaquopy 版本表](https://chaquo.com/chaquopy/doc/current/versions.html)、[TV Material 版本记录](https://developer.android.com/jetpack/androidx/releases/tv)。

## 后续

P2 进行中：先收口页面状态与源 / 存储访问，旧 UI 继续可用。完整 ARMv7 真机、外部线路、直播与音频验证仍待完成。
