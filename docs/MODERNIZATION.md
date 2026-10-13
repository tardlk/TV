# 现代化迁移记录

方案与任务：[tv-modernization](../.scratch/tv-modernization/spec.md)。用户已授权按阶段执行；保留既定结构与已可用能力，视觉按 [Material UI 规格](../.scratch/tv-material-ui/spec.md)推进，Debug 验证通过后提交。

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

P2 初始接口与状态、P3 首页核心功能已通过，P4 点播页面接入进行中。完整 ARMv7 真机、外部线路、直播与音频验证仍待完成。

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

通过脚本 `python3 scripts/check-device.py` 执行当前五组设备检查；前提是已安装对应主包与 AndroidTest 包，并使用独立测试设备。脚本检查 instrumentation 的 OK / FAILURES 文本，不能只看 adb 的退出码。

## P4：历史与收藏切片

历史 / 收藏已改为 Kotlin Activity 和共用 LibraryViewModel。不可变 StateFlow 驱动列表，LibraryStore 继续委派 Room；删除使用 sourceKey + vodId，读取和写入在 IO 线程执行，写操作序列化，晚到的旧读取不能覆盖新列表。确认清空弹窗仅回调当前页面操作，不持有静态 Activity adapter。缺失站点的搜索回退、长按搜索、收藏详情入口以及 push_agent 虚拟站点保留原语义。

原电视列表、按钮与页面布局仍作为兼容 View 保留。整页 AndroidView 的候选包装实测使方向键焦点留在 Compose 宿主；依照方案回退到原生 View 宿主，删除该包装，不发展新的焦点框架。状态和存储迁移继续保留，详情与搜索尚未完成，本票没有标记完成。

验证：双 ABI 构建、17 项单元测试（首页 9、列表 8）通过。源引擎 3、源接口 2、顶部导航 1、固定源详情 / 播放 1、列表操作 1，共 8 项设备测试通过。列表测试在真实 Room 上创建 UUID 隔离记录，验证向上进入删除按钮、遥控器删除指定来源、返回退出删除模式、取消清空、确认清空收藏且不删除历史；finally 删除自己的记录。界面测试必须串行执行。

1080p 浅色对照：收藏像素一致；历史有 134346 个像素差异，全部位于同一张卡片内（x84–418、y142–579），来自原 ImgUtil 的随机字母占位图颜色；文字、标题、toolbar 与卡片位置相同。并排截图和 JSON 留在 artifacts/modernization/p4-before、p4-library-after。其余尺寸 / 主题矩阵仍待补充。

本切片 Debug 包在 artifacts/modernization/p4-library/，保留 artifacts 根目录的 P3 对照包。没有安装新工具或新增自动启动项；临时 AVD / adb 已在本次 P4 切片交付后停止并清理，最新留存见 ENVIRONMENT.md。

## P4：详情信息与收藏切片（0.2.1-dev）

标题、来源 / 年份 / 地区 / 语言 / 类型、演员、导演及三行简介改为 Compose，使用 DetailViewModel 的不可变状态。详情 / 换源 / fallback 请求仍由原 SourceViewModel 协调，一次已接受的结果供给展示状态；此次没有引入第二条详情请求或播放解析链。选集、线路、排序、复制播放地址、顶部导航、原 PlayFragment 和 VodController 继续在原生宿主中工作。详情 Activity 的完整业务协调、搜索迁移和页面重建验收仍未完成，P4 保持进行中。

收藏经 LibraryStore / Room 读取与写入，读写串行且拒绝旧来源晚到的状态；只有落盘成功才变更已收藏状态。连续确认期间保留原按钮焦点，由 ViewModel 忽略重复请求；候选曾通过禁用按钮防重复，设备回归发现原生焦点跳回顶部，已取消该做法。回归保留连续两次确认及真实 Room 结果断言。

简介行距使用原生 StaticLayout 的 fallback 字体度量，保留原 XML 的 1.2 倍行距和 3mm 附加间距；API 28 即支持所用 [fallback 行距接口](https://developer.android.com/reference/android/text/StaticLayout.Builder#setUseLineSpacingFromFallbacks(boolean))。单行和原首页的度量逻辑保持。

### 验证与交付

- Python32 / Python64 Debug 和 AndroidTest 构建通过；23 项单元测试（首页 9、历史 / 收藏 8、详情 6）全部通过。
- 最终 0.2.1-dev 安装后，五组共 8 项设备测试通过，涵盖原 JAR / JS / Python、真实源接口、导航、详情收藏 / 播放、列表删除 / 清空。设备检查串行运行，报告在 artifacts/modernization/p4-checkpoint-tests。
- 旧版与候选四组 720p / 1080p、浅 / 深色各执行固定源详情、连续收藏切换、预览 → 全屏、媒体键暂停 / 恢复。保持同一播放 Fragment 和视频 View，收藏重组 / 全屏切换未增加详情请求。此证据不等于服务托管已完成，也不覆盖页面重建或音频。
- 排除动态视频和时钟后，四组静态像素差异约 0.81%–1.41%；全部位于迁移的信息区，信息区之外像素一致。字号、折行、三行省略、行距和原生按钮位置对照通过，不宣称字体逐像素相同。并排图、原图与 JSON 在 artifacts/modernization/p4-detail-matrix。
- 交付 artifacts/TV-0.2.1-dev-armv7-debug.apk、TV-0.2.1-dev-arm64-debug.apk（versionCode 3），包名 / Android 9 下限 / 精确 ABI / QuickJS / Python / 无预置源 / 无测试订阅 / 无测试视频与图片检查通过，APK v2 签名验证通过。原 0.1.0 / P3 包仍保留。

测试图片与视频仅在 AndroidTest assets。暂时没有 ARMv7 真机；性能、完整生命周期、外部线路和直播仍按原任务跟踪，不能把本切片当作九阶段已经全部完成。

## P4：普通搜索状态与请求切片（0.2.2-dev）

普通搜索由 SearchViewModel / StateFlow 持有搜索范围快照、调度、精确 / 前缀匹配、目录栈、暂停恢复、选中索引与详情候补；SourceAccess.folder 委派原 getList。SearchActivity 继续拥有原 XML、左侧 SearchKeyboard、词表、搜索历史 / 指定源和原生 TV 列表，只收集状态与执行导航。已替代的线程池、计时器和搜索结果 EventBus 入口移除。FastSearchActivity 的分词 / 按源筛选和原调度保持，默认搜索模式保持。两页行为差异和边界见 [搜索基线](../.scratch/tv-modernization/baseline/search.md)。

搜索桥的阻塞调用使用独立有界 worker（API 28 / 30 / 35 分别 12 / 18 / 24），避免占住原共用单线程目录 / 详情 / 播放队列。原 HTTP 搜索通过新增的 getSearchForRequest 入口使用 UUID tag，其余源协议和旧调用签名保持；取消注销该请求的事件监听并中断 worker / 取消该 HTTP tag，不再取消全局搜索或所有 JS 请求。第三方调用可能忽略中断并继续占用有界 worker；代次拒绝晚到结果，不能把 Job 取消描述为源调用已停止。JS 内部 HTTP 共用 tag 的完整收口待 P8。

目录与详情返回使用原 TvRecyclerView 的 selection 和 itemView.requestFocus 恢复原结果位置，不扩展焦点框架。原版设备对照确认目录返回落到左侧按钮；候选保留页面几何和原生焦点样式，增加局部返回恢复。Activity 配置重建保留 ViewModel 的结果和查询，不重发已完成搜索；进程死亡恢复尚未验证。

### 验证与交付

- Python32 / Python64 Debug 和 AndroidTest 构建通过；34 项单元测试（首页 9、历史 / 收藏 8、详情 6、普通搜索 11）全部通过。搜索覆盖范围 / 首页优先、空选择、HTTP 立即启动、六个初始 spider / 3 秒推进 / 有界并发、阈值、候补身份、异常 / 超时、暂停恢复、晚到请求拒绝和目录返回。
- 最终 0.2.2-dev 在临时 API 33 ARM64 TV 上串行执行六组、11 项设备测试通过：源引擎 3、源桥接 3、首页导航 1、原详情 / 播放 1、列表删除 / 清空 1、搜索 2。新增桥接测试在慢 JAR 调用期间验证 HTTP 目录与另一项 JAR / JS / Python 搜索返回；目录走真实原 getList 参数。搜索测试覆盖原键盘确认 / 删除、远程输入、action / tag 字段、目录加载 / 返回、详情入口 / 返回、配置重建不重复搜索、快速搜索路由。原预览 / 全屏和媒体键暂停恢复回归通过。
- 旧 0.2.1-dev 与候选在 720p / 1080p、浅 / 深色、文字 / 海报共八组运行相同固定源测试；普通结果页和快速结果页共 16 对截图像素一致。八对目录页约 1.22%–1.38% 像素差异均由原生焦点高亮位置变化造成，检查并排图确认没有布局 / 字体 / 图片变化。矩阵包含 32 次设备测试执行，作为对照证据，不与最终 11 项回归重复累计。
- 交付 `artifacts/TV-0.2.2-dev-armv7-debug.apk` 与 `TV-0.2.2-dev-arm64-debug.apk`（versionCode 4）。包身份 / API 28 / 精确 ABI / QuickJS / Python / 无预置源 / 无用户测试订阅 / 无测试视频与图片 / APK v2 签名检查通过；原包全部保留。报告、测试 APK 与单测在 `artifacts/modernization/p4-search`，最终设备报告在 `p4-search-final-tests`，对照截图 / JSON 在 `p4-search-matrix`，均为 ignored 本地证据。

这是一组普通搜索状态与请求迁移，原生 UI 宿主保留；不是快速搜索或整个 P4 完成。快速搜索状态、词表请求生命周期、详情完整协调、进程死亡与完整错误 / 空态矩阵继续待做。ARMv7 真机、外部线路、直播、音频和既有性能问题未在本轮获得新结论。

未安装工具或新增自动启动项；复用专用构建环境与 Codex 已有 Pillow / NumPy 作像素比较。临时 TV_P4_Search_Test、本任务启动的 adb 5039 / 5037、构建中间产物和一次性脚本已停止 / 清理，长期 TV_1080p_API33 数据未改。最新空间记录见 ENVIRONMENT.md。

## Material 3 for TV：首页与设置样板（0.3.0-dev，2026-10-13）

用户提出用 Compose 达到现代 Android Material 3 观感后，先统一主题与常用控件，保留 C 首页和成熟操作。TvTheme 按系统浅 / 深色选择基底，映射原暖色 XML 语义色，统一字体层级与形状。导航胶囊、首页推荐信息、轻量渐变和主操作应用新主题；原 ImgUtil、原生分类 / 继续观看列表、详情 / 播放器宿主继续复用，没有新增模糊、动态取色或图片来源依赖。

设置显示层改为 Compose，29 个旧 action ID 全部映射到四个类别；条件线路行与四次零键显示调试入口保留。TvSettingValue 仅供 Compose 显示原协调者提交的文字，ModelSettingFragment 继续拥有原配置 / SAF / 弹窗 / Hawk / 播放器设置与联动，不引入第二个设置后端。单项 500ms 防重复确认保持按钮可用与焦点。原隐藏单菜单的 ViewPager 宿主会在长列表方向导航时平移整个窗口，替换为 FragmentContainerView 后标题和类别栏稳定，列表只在右侧滚动。旧 fragment_model.xml 暂留为原 ID 声明和对照，运行入口已替换。

### 验证与本地交付

- Python32 / Python64 Debug、AndroidTest 和原 34 项单元测试通过；包身份 / API 28 / 精确 ABI / QuickJS / Python / 无预置源 / 无用户测试地址 / 无测试视频图片或动态源样本 / APK v2 签名检查通过。版本为 0.3.0-dev，versionCode 5，原包保留。
- 最终 ARM64 API 33 TV 串行设备测试 13 项通过：源引擎 3、源桥接 3、顶部导航 1、原详情 / 播放 1、列表 1、搜索 2、设置 2。新增设置用例覆盖所有 29 入口、类别左右交接、长列表标题固定、原配置 / 播放器 / 搜索布局弹窗、返回焦点、连续确认只写一次、值刷新与 Activity 重建。界面显示改为 Compose 后，旧按文本查询的导航测试改用稳定资源标签；API 33 测试读取前清 accessibility 缓存。
- 720p / 1080p、浅 / 深色共四组，各执行空壳六页、固定源首页 → 详情 / 收藏 / 同一预览与全屏 / 媒体键暂停恢复及设置全部入口 / 落盘重建，共 16 次矩阵测试执行，保存 72 张截图。视觉检查确认焦点行完整可见、标题与类别栏固定；长列表非焦点行在视口边缘的自然裁切不属于焦点裁切。该矩阵验证新视觉规范，不再要求新旧逐像素相等。
- 同临时设备、空首页、深色 1080p、各 5 次冷启动，0.2.2-dev → 0.3.0-dev 的 am start TotalTime 中位数为 359 → 360 ms（+0.28%）；启动后 5 秒 PSS 为 130168 → 136388 KiB（+4.78%）。这是最近版本的视觉增量对照，不能替代可操作首帧、Release、ARMv7 或前述 P3 相对最早基线的性能验收。
- 本地双 ABI 包在 artifacts 根目录，构建 / 单测 / 签名报告、最终设备日志、矩阵、预览拼图和性能 JSON 在 ignored artifacts/material-ui。临时 AVD / adb、构建中间产物、测试生成 JAR 与一次性脚本清理；环境与占用见 ENVIRONMENT.md。只做本地提交，没有推送、Release 或 APK 上传。

本轮是视觉样板，等待用户审阅后逐页推广；不代表 P4 / P5 已完成。快速搜索状态、词表生命周期、详情完整协调、设置完整状态迁移、直播和播放服务仍按原任务推进。ARMv7 真机、音频、真实外部线路及完整生命周期继续待验收。
