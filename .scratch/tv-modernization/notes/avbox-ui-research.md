# AVBox UI 特点源码调研

日期：2026-10-13。范围：只读参考 AVBox 的实际 UI 源码；没有启动应用、构建、安装工具或修改 AVBox。TV 当前 UI 与迁移路线保持原约定，本笔记不构成改版授权。

本地 AVBox 基线为 `b0c4eecf42ca03c2cb035f9d63685d92de4a39c5`，与本地 `origin/main` 相同。线上 main 已有局部后续修改；下面链接固定在本地基线，避免混淆。文件路径相对 AVBox 根目录，审美判断另用“分析”标识。

线上 main 的 `HeroSpotlight.kt` 与本地视觉结构一致，图片尺寸接口已由本地 `preferLarge` 改为 `rememberHeroImageSizes().poster` / `size`；不能把本地文件行号当作线上 main 的实时行号。本次主代理另查看 README 展示图并保存对照拼图 `artifacts/research/avbox-ui/readme-contact-sheet.jpg`；README 截图是展示快照，不能证明最新版本所有页面、配置和设备上都相同。

## 已实现的特点及视觉作用

1. **统一的语义配色和字体层级。** `AVBoxTheme` 统一提供 Material 3 ColorScheme 与 AVBoxTypography，支持深浅模式、种子色、纯黑；系统取色在 Android 12+ 使用 dynamicLight/DarkColorScheme，自定义取色使用 MaterialKolor。卡片色统一映射 `surfaceBright`，不是各页随手挑色。字体包含 24sp/32sp 粗体大标题、18sp/26sp 半粗体章节、16sp/24sp 正文、14sp/20sp 次级正文。**分析：**色彩、文字、容器有一致层级，是现代感的基本来源；使用 Kotlin/Compose 本身不等于好看。
   - 证据：`ui/theme/Theme.kt:43–73`、`AppThemeState.kt:70–93`、`Color.kt:9–10`、`Type.kt:8–40`（这些路径均位于 `app/src/main/java/com/github/tvbox/osc/`）。
   - 上游：[主题](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/theme/Theme.kt#L43)、[取色](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/theme/AppThemeState.kt#L70)、[字体](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/theme/Type.kt#L8)。

2. **首页由大海报建立视觉焦点，再进入小海报内容流。** 当前横向模式首页实际调用 `HeroSpotlight`，高度为屏高 0.58，限制 360–560dp；全宽大海报下半部多段渐变融进页面，底部是居中大标题与胶囊页码点。分区内容为标题 + “全部”胶囊 + 横向海报；外边距 16dp、卡间距 12dp。**分析：**大小、疏密与章节节奏鲜明，让页面更像内容产品而不是同样大小的按钮表格。
   - 证据：`ui/page/HomePage.kt:320–345,633–651,702–712`；`ui/components/HeroSpotlight.kt:49–70,119–208,212–260`。
   - 上游：[首页结构](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/page/HomePage.kt#L320)、[Hero](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/HeroSpotlight.kt#L49)。

3. **海报、背景、配色相互呼应，边界柔和。** Hero 图片取种子色；首页背景色保留色相、限制饱和度到 0.14，浅色亮度 0.96、深色 0.24，200ms 过渡。Android 12+ 横向首页把海报放大、40dp 模糊为全页底景，再用渐变蒙层控制可读性。详情也实际调用 `PosterBackdrop`，56dp 模糊 + 1.3 倍放大 + 多段蒙层；详情卡片使用 50% 透明 surfaceContainerLowest。**分析：**图片不再贴在生硬的纯色底上，背景带内容色调但保持克制，构成空间深度。
   - 证据：`ui/page/HomePage.kt:165–179,272–273`；`ui/components/HomeBackdrop.kt:5–27`、`HomeHeroBackdrop.kt:22–88`、`PosterBackdrop.kt:18–57`；`ui/activity/DetailScreen.kt:185–200`、`DetailSections.kt:68–72`。
   - 上游：[背景取色](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/HomeBackdrop.kt#L5)、[首页模糊](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/HomeHeroBackdrop.kt#L22)、[详情底景](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/PosterBackdrop.kt#L18)。

4. **胶囊控件与真正的玻璃导航。** 搜索框使用 ContinuousCapsule；首页搜索钮为 40dp 圆钮。`GlassTopBar` 使用 backdrop 采样、模糊、饱和度、表面色透明层、高光、8dp 阴影；Android 13+ 还可有 lens 折射与色散，Android 12 以下或关闭玻璃时回退普通容器色。主屏实际挂载 `FloatingNavBar`，胶囊与边缘留白令导航浮在内容上；关闭玻璃后用 ShortNavigationBar 或 NavigationRail。**分析：**现代感来自控件形状、透明层与空间关系的系统化组合；不能声称所有手机、所有页面都能看到全部玻璃效果。
   - 证据：`ui/components/SearchField.kt:62–76,99–100`、`GlassTopBar.kt:57–106`；`ui/page/HomePage.kt:255–269`、`MainScreen.kt:258–279,371–398,402–407`；`ui/navbar/FloatingNavBar.kt:294–316`。
   - 上游：[玻璃](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/GlassTopBar.kt#L57)、[主屏导航](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/page/MainScreen.kt#L371)。

5. **统一圆角与更少的图片内文字。** 实装 VodCard 两种样式：Overlay 是图内渐变文字；Stacked 是海报与外置单行居中标题。当前首页分区与首页竖向网格使用 Stacked；两种都为 2:3 海报、16dp 圆角。设置卡片按组采用外围 32dp、大组内部 4dp 圆角，组边界明确。**分析：**统一形状语言、海报不被大量信息盖住，有助于干净感；32dp 和 4dp 的差异也在表达“整组”和“单行”。
   - 证据：`ui/components/VodCard.kt:28–89`；`ui/page/HomePage.kt:707–712`、`HomeGridLayout.kt:245–249`；`ui/components/SettingsGroup.kt:88–92`。
   - 上游：[海报卡](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/VodCard.kt#L28)、[设置分组](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/SettingsGroup.kt#L88)。

6. **详情页有主次明确的操作与内容。** 大海报渐隐，居中标题/元信息，下方 primary 色的 52dp 高播放胶囊，次级动作独立放到圆钮行。TMDB 元数据提供评分、简介、类型 chips；演员横向列表使用 64dp 花瓣形头像。播放器也使用统一图标、预览/全屏尺寸调整、透明黑胶囊信息块。**分析：**主要操作突出、次级操作收敛，详情像完整媒体产品；头像与分集图等真实素材比字体框列表更生动。
   - 证据：`ui/activity/DetailHero.kt:95–135,190–218,252–321`、`DetailTmdbSections.kt:81–194`；`player/ui/PlayerTopBar.kt:45–59`、`PlayerBottomBar.kt:307–318`。
   - 上游：[详情 Hero](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/activity/DetailHero.kt#L252)、[演员与元数据](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/activity/DetailTmdbSections.kt#L81)、[播放器信息胶囊](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/player/ui/PlayerBottomBar.kt#L307)。

7. **小幅动效与加载/失败状态也被设计。** 共用 PressableCard 按下缩放到 0.97、弹簧回弹并保留 ripple；骨架 shimmer 为 1000ms 线性循环。首页按分区表现骨架、空态、错误和重试；海报失败回退图片链，最终有片名首字彩色占位。**分析：**点击得到反馈、加载仍保留布局、失败有一致表现，成熟感不仅存在于静态截图。
   - 证据：`ui/components/PressableCard.kt:33–53`、`Skeleton.kt:25–48`、`VodPoster.kt:63–71,86–133`；`ui/page/HomePage.kt:657–699`。
   - 上游：[按压](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/PressableCard.kt#L33)、[骨架](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/Skeleton.kt#L25)、[海报回退](https://github.com/XiaochangXu/AVBox/blob/b0c4eecf42ca03c2cb035f9d63685d92de4a39c5/app/src/main/java/com/github/tvbox/osc/ui/components/VodPoster.kt#L86)。

## 规范与源码的区别

- `skill/avbox-mobile-ui-spec.md:67–70` 仍写“Hero 大卡”“无评分角标”“只显示年份”。本基线代码首页实际调用 HeroSpotlight，VodCard 已绘制 RatingBadge，Overlay 元信息拼接年份/地区。报告以实际调用和源码为准。
- `HeroCarousel.kt` 虽仍存在，不能拿未调用组件说明当前首页。当前 HomePage 的 Hero 入口是 HeroSpotlight（见第 2 项）。
- 本次未在设备上逐项验证性能、动画效果与所有主题；“为什么好看”的部分为对源码视觉结构的分析，不是普适审美事实。

## TV 可借鉴的设计原则

设计分析：最值得借鉴的是大图/小图层级、文字与留白节奏、主题色与内容的呼应、组件一致性、主操作突出；玻璃与花瓣头像是个性化细节，不是现代感的全部原因。手机版底部导航、触摸尺寸不应直接搬到 TV。按现有 TV 约定保持 C 首页、左侧搜索键盘、已可用的遥控器与播放器逻辑；本研究不启动实现。

## 环境记录

没有新增工具、环境、缓存、后台服务或自动启动项；只新增本笔记。没有启动项目服务，没有修改参考仓库，没有提交或推送。
