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

P2 初始接口与状态已通过，P3 首页接入进行中。完整 ARMv7 真机、外部线路、直播与音频验证仍待完成。

## P2：页面状态与旧源桥接

新增 Kotlin 不可变内容模型、SourceAccess、LibraryStore、SettingsStore 与 HomeViewModel。源访问委派原 SourceViewModel；Room / Hawk 继续作为唯一现有后端；近期记录和收藏经 IO 线程访问。原页面尚未改动。

8 项 HomeViewModel 单元测试覆盖空壳、推荐 / 历史、空结果、错误、取消、不可中断旧请求晚到、复用原请求种子和错误来源拒绝。旧站点晚到不能覆盖新状态，种子数据不发第二次首页请求。

真实源集成测试经过原 ApiConfig 加载临时 loopback 配置，再经原 HTTP / 解析链执行目录、搜索、两次并发搜索、详情与播放解析；第二项验证收藏的 sourceKey + vodId 标识。2/2 通过，服务在 finally 停止，触碰的设置恢复。

曾稳定复现搜索回调超时：HTTP 与解析已完成，但旧 SourceViewModel 的搜索结果经 EventBus 分发，不进入 searchResult LiveData。修复只在新 adapter：按站点与 UUID token 接收原事件，结束 / 取消注销；未修改原事件或源实现。并发搜索回归测试确认结果不串。测试断言保留阶段与自有请求路径，生产代码无临时调试日志。

目前有一个上游行为限制：部分目录 / 搜索解析错误在旧层已归一为空结果，adapter 无法还原错误原因。状态层能报告明确异常和超时，保留旧空结果语义，P8 拆原加载模块时再收口。这不影响正常源链与本轮状态测试通过。

## P3：Compose 首页核心迁移

顶部导航与主推荐区域使用 Compose for TV，保持当前 C 结构、尺寸、配色、圆角、字重和动作。复用 NativePaint 字体度量，消除 Compose 中文 fallback 行高引起的纵向偏移；通用文本 / pill 供后续页面复用。图片仍调用原 ImgUtil，分类和继续观看列表保留原生电视控件及删除 / 长按等成熟操作。原 HomeActivity 继续负责现有配置与分类流程，推荐结果作为种子进入 HomeViewModel，避免重复请求。没有改动播放器与直播控制器。

验证：Python32 / Python64 Debug、9 项状态单测通过；分进程执行动态源 3 项、原源 / 收藏接口 2 项、顶部导航 / 搜索 / 设置 1 项、固定源推荐到原详情 / 全屏播放及媒体键暂停恢复 1 项，共 7 项设备测试通过。AndroidView 互操作后的 accessibility focused 标记有时不反映已显示的按钮焦点，跨区域流程以实际方向键 / 确认键动作与播放结果验证；未据此重写焦点框架。

空壳四组浅深色 / 720p / 1080p 对照中，主要几何和色彩保持，约 0.56%–1.08% 像素差异主要来自字体栅格化和边缘绘制，不宣称新旧逐像素相同。固定源卡片、详情与全屏截图留在 artifacts/modernization。完整加载 / 错误 / 多记录矩阵仍待补充。

性能实测：5 次 Debug 空首页，冷启动 TotalTime 中位数 349 ms，PSS 128433 KiB；原基线 243 ms / 106696 KiB，约 +43.6% / +20.4%。P1 构建升级后的对应值为 269 ms / 127089 KiB，P3 在此基础上约 +29.7% / +1.1%。新 UI 功能通过不表示达到建议性能门槛；后续需要启动剖析与 ARMv7 实测。用户确认暂时没有 ARMv7 设备，相关硬件验收保留未完成。

可安装对照包：artifacts/TV-0.2.0-dev-armv7-debug.apk 与 TV-0.2.0-dev-arm64-debug.apk。原 0.1.0 包仍保留。测试用 117 KiB 自制 H.264 文件仅在 AndroidTest assets 内，正式 APK 不包含；其生成源码为 tests/fixtures/make-clip.swift。

通过脚本 `python3 scripts/check-device.py` 执行当前四组设备检查；前提是已安装对应主包与 AndroidTest 包，并使用独立测试设备。脚本检查 instrumentation 的 OK / FAILURES 文本，不能只看 adb 的退出码。
