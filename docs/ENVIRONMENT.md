# 开发环境与留存

本文件记录本次为 TV 使用或创建的环境，不改变其他项目共用工具。

## 检查结果

- 已有 Git、GitHub CLI、系统 Python 3.9.6，以及 Codex 提供的 Node / Python / Playwright。
- 未发现可用 Java 运行时、Android SDK、sdkmanager、adb 或 Gradle 用户环境。
- 主机为 macOS ARM64。ARMv7 APK 的构建与包内原生库可在本机核验；实际 ARMv7 播放需在对应电视或盒子验证。

## TV 专用目录

统一位于 `${HOME}/.local/share/tv-android/`，不写入 shell 启动文件：

| 子目录 | 用途 |
| --- | --- |
| `jdk-17/Contents/Home` | Gradle 7.5 与 Android 命令行工具使用的 JDK |
| `sdk/` | Android SDK 平台与构建工具 |
| `gradle-cache/` | 本工程独立 Gradle 分发与依赖缓存 |
| `python-3.8/` | Chaquopy Python 3.8 的匹配构建解释器 |
| `sdk/emulator` 与 `sdk/system-images/android-30/google_apis/arm64-v8a` | 后续原生 UI / 播放测试 |

JDK 来自 Eclipse Temurin 官方 GitHub release；Android 命令行工具来自 Google SDK 仓库，安装前核验官方校验值。记录文件为该专用目录内的 `install-record.json`。

首包保留 TVBoxOS 的 Gradle 7.5 / AGP 7.2.2；现代化工具版本以末尾补充为准。Python 构建命令通过 `TV_BUILD_PYTHON` 或 `-PbuildPython` 传入，替换上游 Windows 路径。

Mac ARM64 的额外兼容处理：

- Room 2.3.0 编译器原有 SQLite JDBC 不含 Mac ARM64 原生库；仅在 `annotationProcessor` 路径使用 `sqlite-jdbc:3.45.3.0`，应用的 Room runtime 仍为 2.3.0。
- 已通过系统 `softwareupdate` 安装 Rosetta 2，供上游 Intel 架构 Android 构建工具运行。它是系统共用组件，不设置本项目自动启动项，不在任务清理时删除。安装前执行 Intel 程序返回 `Bad CPU type`，安装后执行成功。

## 服务与清理

不设置自动启动项。构建使用 `--no-daemon`，完成后检查并停止本任务启动的 Gradle / adb 等临时进程。安装压缩包与临时测试缓存完成验证后清理；长期保留目录及实际占用在交付时补记。

## 首包交付时留存与占用（2026-10-12）

`${HOME}/.local/share/tv-android/` 总计约 6.1 GiB，供本项目后续构建与测试长期复用：

| 内容 | 实际占用 | 版本 / 用途 |
| --- | ---: | --- |
| JDK | 309 MiB | Temurin 17.0.20.1 ARM64 |
| Python | 54 MiB | python-build-standalone 3.8.19 x86_64，经 Rosetta，用于 Chaquopy 匹配字节码 |
| Gradle 依赖缓存 | 909 MiB | Gradle 7.5 分发与已下载依赖，避免重新下载 |
| Android SDK | 4.9 GiB | cmdline-tools 12、API 33、build-tools 30.0.3 / 33.0.2、platform-tools 35.0.2、emulator 37.2.12、API 30 Google APIs ARM64 测试镜像 |

platform-tools 37.0.1 的 adb server 在本机 macOS 出现 libunwind invalid compact unwind encoding，改用 Google 官方 35.0.2，以 Rosetta x86_64 启动测试 adb server，专用端口 5039。此为宿主工具兼容处理，没有改变应用引擎。匹配 Python 3.8 后消除了构建端 Python 字节码版本警告。

已清理：安装下载 / 解压目录（约 421 MiB）、两个临时 AVD 及设备测试数据（约 1.4 GiB）、不再使用的 ATD 镜像（约 3.1 GiB）、Swift 编译缓存（约 210 MiB）、本地构建中间产物、原始订阅快照和 UI / 媒体日志。APK 与原生截图保留 `artifacts/`，约 142 MiB；忽略目录 `.local-test/` 仅保留用户测试地址、汇总报告和构建日志，约 32 KiB。这些不进入 Git。

已停止：本地测试 HTTP 服务 127.0.0.1:48979、Android 模拟器、adb 5039 / 5037 server；已移除 48978 → 9978 转发。未发现本任务残留的 Gradle daemon。没有新增本项目 LaunchAgent、LaunchDaemon、登录项或 shell 启动配置。

仍保留并可能按系统需要运行：Rosetta 2 的系统共享服务 `com.apple.oahd`，程序 `/usr/libexec/rosetta/oahd`、系统启动定义 `/System/Library/LaunchDaemons/com.apple.oahd.plist`。这是安装 Rosetta 的系统组件，不是项目后台服务，不停止或删除其他项目可能使用的系统工具。已有 Android 调试密钥与系统共享工具保持原状。

上游 GitHub Actions 工作流留作 `test.yml.upstream-reference`，不再作为本工程可执行工作流；本次没有远程发布、推送或 Release 构建。安装来源与校验记录副本见 [environment-install-record.json](environment-install-record.json)。

## Android TV 专用设备补充（2026-10-12）

应用户要求，复用已有模拟器，新增官方 `system-images;android-33;android-tv;arm64-v8a` revision 5（约 4.1 GiB）与长期 AVD `TV_1080p_API33`（约 1.0 GiB）。专用环境现合计约 11 GiB；原普通 Android API 30 镜像仍保留作对照。用途、操作、测试结果及清理见 [TV_EMULATOR.md](TV_EMULATOR.md)。没有新增本项目自动启动项；Rosetta 的系统共用服务继续保留。

## 现代化 P1 构建环境补充

复用 JDK 17、Python 3.8、现有 SDK 与镜像。新增 Android platform 35（130 MiB）、build-tools 34.0.0（187 MiB）、Gradle 8.7 分发（解压与下载缓存约 270 MiB）及 Kotlin / Compose / AGP / Chaquopy 依赖，仍在同一项目专用 Gradle 缓存中。原 Gradle 7.5 留作已交付版本回退构建。安装临时日志将随验证结束清理。

本轮临时 AVD TV_Migration_Test 与 adb 5039 用于原包 / 候选对照，保留的 TV_1080p_API33 未修改；详细迁移和安装提示处理见 [MODERNIZATION.md](MODERNIZATION.md)。运行期间不新增启动项，完成后停止临时服务并删除该 AVD。

## 现代化 P4 切片交付后的留存与清理（2026-10-12）

此处为最新实际状态，前文空间数据是各次交付的历史快照。本切片没有新增安装工具、宿主后台服务或自动启动项。专用目录 `${HOME}/.local/share/tv-android/` 约 13 GiB，长期保留以供后续阶段复用：

| 内容 | 实际占用 | 保留用途 |
| --- | ---: | --- |
| JDK 17 | 309 MiB | 当前 Gradle / Android 工具 |
| Python 3.8 x86_64 | 54 MiB | Chaquopy 匹配构建解释器，经 Rosetta |
| Gradle 缓存 | 2.2 GiB | 8.7 与原 7.5 分发、已锁定构建 / Compose / Python 依赖，避免重复下载 |
| Android SDK | 9.4 GiB | 现有 SDK / emulator / build-tools；API 30 普通镜像约 3.1 GiB，API 33 TV 镜像约 4.1 GiB；platform 35 / build-tools 34 分别约 130 / 187 MiB |
| 长期 AVD TV_1080p_API33 | 998 MiB | 用户要求保留的 TV 模拟器；本轮未改其数据 |

已停止本任务的 TV_Migration_Test 模拟器与专用 adb 5039 server；端口 5039 没有监听，未发现 Gradle / Kotlin daemon。测试内 NanoHTTPD 仅在 instrumentation 进程内运行，测试结束后停止。没有修改 / 停止其他项目服务。

已删除临时 TV_Migration_Test.avd / .ini（约 2.4 GiB）、Swift 模块缓存（约 210 MiB）、本工程各模块 build / .gradle 中间产物及一次性诊断 / 截图暂存。已验证 APK、对照截图、设备 / 单测报告与签名结果在 ignored artifacts，约 835 MiB，保留为阶段审阅和回退证据；含原 0.1.0、P1、P3、P4 包。ignored .local-test 仅约 24 KiB，保留测试地址、源检查摘要和最终构建日志，用户地址不入 Git / 正式 APK。

仍由系统按需运行的共享组件：`/usr/libexec/rosetta/oahd`（Rosetta 2），保留不动；没有本项目仍运行的服务，也没有新增本项目 LaunchAgent、LaunchDaemon、登录项或 shell 启动配置。
