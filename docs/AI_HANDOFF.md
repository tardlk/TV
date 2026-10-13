# AI 接手：TV

更新日期：2026-10-13。应用实现检查点：`0.3.0-dev`（versionCode 5，Material 3 for TV 首页 / 设置样板；对应提交见 Git 记录）。这是一份交接快照，后续修改以当前代码、任务评论与 Git 记录为准。

## 开始工作

1. 先读根目录 [AGENTS.md](../AGENTS.md)，再读[现代化方案](../.scratch/tv-modernization/spec.md)、[Material UI 规格与当前样板](../.scratch/tv-material-ui/spec.md)、[P4 当前任务](../.scratch/tv-modernization/issues/04-compose-vod-pages.md)和[执行记录](MODERNIZATION.md)。完成条件：能区分已运行代码、部分实现与后续计划，知道新视觉目标已覆盖旧逐像素要求。
2. 检查 `git status --short --branch`、`git remote -v` 和已有工具；根据[环境记录](ENVIRONMENT.md)复用环境。完成条件：确认当前分支、用户修改、工具路径及服务状态，再开始改动。
3. 用户要求继续技术迁移时，从 P4 的快速搜索状态迁移接着做；普通搜索状态切片已通过，先读[两页行为差异](../.scratch/tv-modernization/baseline/search.md)。若继续视觉改进，先看 [UI 任务 01](../.scratch/tv-material-ui/issues/01-theme-home-settings.md)与用户对首页 / 设置样板的反馈，再逐页推广主题。完成条件：交付有构建、必要测试与本地提交的切片，更新任务、执行记录与清理结果。

用户已授权按现代化方案执行，无需重新询问路线或重新做需求访谈；新对话的最新指令决定当前动作。最近切片完成普通搜索的状态与请求迁移，仅本地提交；此前 GitHub 整理推送的授权不扩大到后续远程写入。

## 当前进度

| 阶段 | 状态与边界 |
| --- | --- |
| P1 构建链与基线 | 已通过双 ABI 构建、原源引擎固定样本和基线检查 |
| P2 状态与接口 | 已通过；SourceAccess / LibraryStore / SettingsStore 包住原后端 |
| P3 首页 | 顶部导航与推荐区 Compose 核心完成；分类 / 继续观看列表为原生 TV View；完整状态矩阵、性能验收待完成 |
| P4 点播页面 | 历史 / 收藏 Kotlin 状态、详情信息 Compose 与收藏状态、普通搜索 Kotlin 状态已完成切片；快速搜索、词表请求生命周期、详情完整协调、进程死亡 / 完整重建与错误 / 空态矩阵待做 |
| P5 | 设置 Compose 显示层先在视觉样板中完成，29 个旧动作仍由 Java 协调者 / Hawk 拥有；完整设置状态迁移与直播接入未完成 |
| P6–P9 | 播放服务、Media3、内部收敛、最终验收尚未开始；不要据现有同名 Java 类判断已实现新架构 |

P3 尚有收尾项，但 P4 已获授权并在执行；本地任务的 `Blocked by: 03` 是阶段关系，不撤销已记录的执行授权。任务状态仍使用现有分诊标签，不创建另一套进度系统。

## 已确认的产品边界

用户要“重走 AVBox 的演进路线”：以 TVBoxOS 的电视能力为基础，保持已经确认的 TV 界面，逐步采用 Kotlin / Compose / StateFlow / 独立播放会话。不是把 AVBox 手机版硬改为电视版。

产品规范和复用硬约束以 AGENTS.md、现代化方案为准。已确认的选择包括：应用 TV / com.coke.tv；Android 9+、ARMv7 必须保留；C 首页顶部导航、推荐与信息并列、继续观看、搜索键盘左侧；正式应用空壳；个人 / 家庭点播为主，直播能力保留；复杂模糊、透明叠层和动效可以省略。完整辅助页视觉统一另行评审。

用户反复强调：把精力放在界面和架构，直接复用上游可用的遥控器、搜索键盘、播放器与直播控件。优先保留兼容 View；跨区域仅做必要的标准交接，不扩展全局焦点框架。A / B 设计仅为档案，C 已选定。

2026-10-13 用户希望达到现代 Android Material 3 观感，当前首页 / 设置已按新视觉规格统一暖色主题、字体、圆角与焦点。C 首页结构、入口及操作保持，设置可按类别组织；这次样板不代表所有页面已统一。AVBox 的源码 / 视觉研究见 [笔记](../.scratch/tv-modernization/notes/avbox-ui-research.md)，没有照搬手机导航、玻璃或动态取色依赖。

## 实现入口

路径均相对仓库根目录。

| 区域 | 入口与现有所有者 |
| --- | --- |
| 首页 | `ui/activity/HomeActivity.java`、`ui/fragment/UserFragment.java`；原配置 / 分类流程保留，首页结果供给 `modern/home/HomeViewModel.kt` |
| Compose | `ui/compose/TvHeaderView.kt`、`TvHeroView.kt`、`TvControls.kt`、`TvTheme.kt`；尺寸沿用原 mm / AutoSize 转换，不能直接当 dp |
| 统一接口 | `modern/data/SourceAccess.kt`、`LegacySourceAccess.kt`、`LibraryStore.kt`、`SettingsStore.kt`；不可变模型在 `modern/model/Content.kt` |
| 历史 / 收藏 | `ui/activity/LibraryActivity.kt`、`modern/library/LibraryViewModel.kt`、`LegacyLibraryView.kt`；Room 是唯一存储后端，原生列表宿主保留 |
| 详情 | `ui/activity/DetailActivity.java` 仍拥有请求 / fallback / 选集和原生播放宿主；`modern/detail/DetailViewModel.kt` 仅管理已接受的信息快照和收藏；`ui/compose/TvDetailInfoView.kt` 显示信息 |
| 普通搜索 | `modern/search/SearchViewModel.kt` 持有调度 / 匹配 / 目录 / 候补；`ui/activity/SearchActivity.java` 保留原输入 / 词表 / TV 列表宿主；`SourceAccess.folder` 委派原 getList |
| 快速搜索（下一步） | `ui/activity/FastSearchActivity.java` 的分词 / 按源筛选 / 调度仍是原实现；`ui/tv/widget/SearchKeyboard.java` 保留不动；行为迁移前完整阅读相关方法和原布局 |
| 设置 | `ui/activity/SettingActivity.java` 使用 FragmentContainerView；`ui/fragment/ModelSettingFragment.java` 保留全部动作，`ui/compose/TvSettingsCatalog.kt` 映射 29 个入口、`TvSettingsView.kt` 显示 / 局部导航 / 防重复确认；TvSettingValue 只承载显示值，没有第二套持久化 |
| 源与播放 | `viewmodel/SourceViewModel.java`、`api/ApiConfig.java`、`ui/fragment/PlayFragment.java`、`player/`；契约包名 / 类名 / 字段 / 反射签名保留 |

表中 app 代码路径均位于 `app/src/main/java/com/github/tvbox/osc/`。应用 ID 与源码 namespace 不同是为动态源契约兼容保留的选择。

普通搜索已经核对并迁移指定来源 / 首页优先、精确 / 前缀匹配与阈值、并发 / 超时、暂停 / 恢复、详情候补和目录栈；保留左侧原 SearchKeyboard、词表和原生 TV 结果列表。配置重建不重复搜索，目录 / 详情返回通过原列表恢复结果焦点。快速搜索还有按空白分词匹配、按源筛选、2 秒 pump 与状态栏等差异，不直接套用普通搜索匹配规则。词表请求、进程死亡和完整错误矩阵尚未迁移 / 验收；替代通过后才移除旧入口。

## 已遇到的问题

- 原 `SourceViewModel.getSearch` 通过 EventBus 返回结果，不向公开的 `searchResult` LiveData 发布。`LegacySourceAccess` 已按 sourceKey + UUID token 订阅并注销；继续复用该桥接和并发搜索回归测试。
- 普通搜索接入后，adapter 搜索使用独立有界 worker，避免占住旧 spThreadPool；HTTP 搜索使用 `getSearchForRequest` 的独立 UUID tag。取消只作用于该请求，不能退回全局 `cancelTag("search")` / `JsLoader.stopAll()`。第三方阻塞调用可能忽略中断，继续占用有界 worker；JS 内部 HTTP 共用 tag 的进一步收口待 P8。
- `push_agent` 是可用的虚拟来源，不进入配置来源列表；可用性用 `SourceAccess.supports` 判断。历史 / 收藏身份是 sourceKey + vodId，不能只按 ID 去重或删除。
- 整页 AndroidView 包装原生历史列表曾让焦点落在 Compose 宿主，已回退为原生列表宿主；这个取舍已经过设备对照。详情收藏保存期间禁用按钮也会丢焦点，当前由 ViewModel 忽略重复确认并保留按钮可用状态。
- `TvControls.TvText` 继续为详情使用原生字体度量与三行 fallback 行距；首页、胶囊和设置采用新 Material 字体。替换剩余旧文本前对照浅 / 深色、720p / 1080p；原 ImgUtil 的字母占位图颜色每个进程会随机变化。
- 设置旧单页 ViewPager 会在 Compose 长列表焦点移动时带动整个窗口平移；已经换成标准 FragmentContainerView。API 33 accessibility 缓存可能让测试读取旧 Compose 文本 / 焦点，测试通过重新设置 UiAutomation.serviceInfo 清缓存并递归查找资源标签；不据此改生产焦点逻辑。

## 已验证与待验收

检查点已通过：Python32 / Python64 Debug、包名 / API 28 / 精确 ABI / QuickJS / Python、APK v2 签名，以及无预置源、无用户测试订阅、无测试视频 / 图片。

- 单元测试 34 项：首页 9、历史 / 收藏 8、详情 6、普通搜索 11。
- ARM64 API 33 Android TV 最终设备测试 13 项：动态 JAR / JS / Python 3、原源接口与搜索 worker 3、顶部导航 1、固定源详情收藏 / 预览与全屏 / 暂停恢复 1、列表删除 / 清空 1、普通搜索 / 快速搜索路由 2、设置全部入口遍历及原弹窗 / 值写入 / 重建 2。
- 详情旧版 / 候选的 720p / 1080p、浅 / 深色四组对照通过；排除视频和时钟，信息区以外像素一致，信息区存在字体栅格差异。
- 搜索旧版 / 候选的 720p / 1080p、浅 / 深色、文字 / 海报共八组对照通过；普通与快速结果页 16 对截图像素一致，目录页差异仅局部焦点恢复后的原生高亮。搜索设备测试验证原键盘确认 / 删除、远程输入、action / tag 字段、目录 / 详情返回、配置重建不重复搜索和快速搜索路由。普通搜索仍是原生 UI 宿主；不宣称完整 Compose 页面已完成。
- 当前 Material 样板四组尺寸 / 主题分别验证空壳六页、固定源首页 → 详情 / 原播放及全部设置项；16 次矩阵测试执行通过。截图有空配置与固定内容，长列表滚动仅影响右侧设置区域，焦点行完整可见。截图中的测试内容只在设备 / AndroidTest 中，正式 APK 仍为空壳。

单测和设备测试源码在 `app/src/test`、`app/src/androidTest`；固定源、视频和图片为自有测试样本，只进入测试 APK。测试会写数据和切换配置，应使用独立测试设备；清空用例要求测试设备收藏为空。设备界面测试必须串行执行。

性能仍未达建议门槛：同一临时设备的 Debug 空首页，P3 冷启动 TotalTime 中位数 349 ms，原基线 243 ms；PSS 128433 KiB，原基线 106696 KiB（约 +43.6% / +20.4%）。这是既有测量快照，不是可操作首帧、Release 或 ARMv7 结论。方法见执行记录与基线行为表。

本次同设备 / 同内容各 5 次对照最近 0.2.2-dev：TotalTime 中位数 359 → 360 ms（+0.28%），启动后 5 秒 PSS 130168 → 136388 KiB（+4.78%）。这只衡量视觉切片的增量，不推翻前述既有性能问题；原始 JSON 在 artifacts/material-ui。

用户确认暂时没有 ARMv7 设备，不重复索要。完整生命周期、服务所有权、真实外部线路 / 直播和音频验证仍待完成；当前自制视频没有音轨。双 ABI 编译不能替代 ARMv7 实际运行。

## 复现与本地材料

先检查环境；本机工具、空间及 ADB 的 Rosetta 兼容处理见 ENVIRONMENT.md，[长期 TV 模拟器操作](TV_EMULATOR.md)仅在需要时读取。构建不会自动安装工具。

```sh
# 双 ABI 主包与状态测试
sh scripts/android-build.sh :app:assemblePython32Debug :app:assemblePython64Debug :app:testPython64DebugUnitTest
# 主包编译后生成测试用动态 JAR，再构建 AndroidTest
sh scripts/build-engine-fixture.sh python64
sh scripts/android-build.sh :app:assemblePython64DebugAndroidTest
# 在独立测试设备安装对应主包 / 测试包后，串行检查；端口 / serial 可用参数覆盖
python3 scripts/check-device.py --serial emulator-5554 --port 5039
# 包检查；可选 --test-url-file 指向 ignored 本地地址文件
python3 scripts/verify-apk.py app/build/outputs/apk/python32/debug/TV_debug-python32.apk --abi armeabi-v7a
```

本机当前 APK：`artifacts/TV-0.3.0-dev-armv7-debug.apk`、`artifacts/TV-0.3.0-dev-arm64-debug.apk`。当前样板证据：`artifacts/material-ui/build`、`final-tests`、`matrix` 与 `preview.jpg`。搜索证据在 `artifacts/modernization/p4-search`、`p4-search-final-tests`、`p4-search-matrix`；此前详情证据与原包仍保留。源码内有[原生基线](../.scratch/tv-modernization/baseline/README.md)和 [HTML 设计档案](../.scratch/design/README.md)。

`artifacts/`、`.local-test/`、`local.properties`、本机 Debug 密钥、SDK / AVD 不入 Git。用户测试地址仅在本机 `.local-test/subscription-url.txt` 或设备数据；不在交接文档中复制。新机器 clone 不会得到本地 APK、候选报告或工具环境，需按源码重建，不能把本机证据路径当作仓库附件。

交接时无本项目运行中的服务，临时 TV_Material_Test / adb、一次性脚本及构建中间产物已清理。长期 `${HOME}/.local/share/tv-android/` 约 13 GiB，保留 JDK / SDK / 匹配 Python / Gradle 依赖及 `TV_1080p_API33`，该长期设备数据未改；本机 APK 与报告约 1.2 GiB。Rosetta 与 Codex bundled Python 等共享工具保留不动。本切片没有安装工具或新增自动启动项。

## Git 与授权

用户仓库：<https://github.com/tardlk/TV>，目标分支 `main`；本地开发分支 `tv/initial-ui` 跟踪 `origin/main`。`origin` 指向用户仓库，`upstream` 指向只读 TVBoxOS；新 clone 默认只有 origin，如需研究上游再按 UPSTREAM.md 添加。

保留上游 Git 历史、许可证与[来源记录](UPSTREAM.md)。项目提交依次为：`a2f5494` 首包、`d1cf993` 模拟器、`854ce3b` 方案、`922e381` P1、`59bb28d` P2、`01bc5cc` P3、`194dd9d` 历史 / 收藏、`f8bb716` 详情切片；交接整理提交可用 `git log` 查询。

此前用户明确授权整理并推送 tardlk/TV。后续普通搜索与 Material 样板按方案执行 Debug 构建和本地提交，没有推送；后续远程写入按当次用户授权处理。Release 构建、GitHub Release、APK 上传、自动发布及对其他仓库的写入没有随此前源码推送获得授权。
