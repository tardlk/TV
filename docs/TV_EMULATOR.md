# 本地 Android TV 模拟器

2026-10-12 配置。复用已有 Android Emulator 37.2.12、platform-tools 35.0.2、JDK 与 Rosetta；仅新增 Google 官方 Android TV 13 ARM64 系统镜像（API 33、revision 5）及一个长期使用的电视 AVD。不是普通 Android 手机镜像。

## 启动与关闭

在仓库根目录运行：

```sh
sh scripts/tv-emulator.sh start
```

设备名 `TV_1080p_API33`，1920×1080。TV 应用与用户提供的测试订阅已保存在此设备的数据分区中。打开电视桌面的 Apps 可找到 TV；也可以在设备启动后运行：

```sh
"$HOME/.local/share/tv-android/sdk/platform-tools/adb" -P 5039 shell am start -n com.coke.tv/com.github.tvbox.osc.ui.activity.HomeActivity
```

关闭模拟器窗口即可结束启动命令，并清理该命令使用的专用 adb server；另一个终端也可运行：

```sh
sh scripts/tv-emulator.sh stop
```

模拟器侧栏的扩展控制中提供方向键 / D-Pad。测试脚本使用 `adb shell input keyevent` 注入方向、确认、返回及媒体播放暂停键，没有增加应用焦点框架。启动参数禁用音频，本轮不涉及音频输出验证。

## 本轮结果

使用提交 a2f5494 对应的 ARM64 Debug APK，未改应用源码或重新构建 APK。

| 检查 | 结果 |
| --- | --- |
| 官方 TV 系统与桌面 | leanback / leanback_only / television 特性存在，电视桌面正常；TV 的 LEANBACK_LAUNCHER 入口可解析 |
| 安装 / 初次启动 | 成功，空壳、1920×1080 深色首页正常 |
| 顶部导航 | 分类向上返回 Home；连续右移可遍历直播、历史、收藏、设置、站源和搜索 |
| 搜索 / 返回 / 设置 | 遥控器可打开，搜索键盘在左侧，返回搜索后可进入设置 |
| 固定配置与详情 | 原配置对话框导入 lives: [] 的本地样本成功；方向键可从首页推荐进入详情 |
| 本地 H.264 播放 | 详情预览与全屏出画面，时间显示 01:00，视频方块移动 |
| 媒体播放暂停键 | 四轮截图采样，运动状态为停止 → 运动 → 停止 → 运动；暂停和恢复通过 |
| 播放控制层 | 方向键可调出原控制层 |
| 历史 | 可打开并出现刚观看的固定视频 |
| 用户订阅 | 通过原对话框导入成功；Android TV 权限提示用确认键允许后，JAR 首页分类、推荐及图片正常 |
| 崩溃记录 | 本轮 AndroidRuntime 错误日志无崩溃 |

暂停采样只比较屏幕中心视频区域，排除时钟、进度条和临时提示；没有用整屏截图差异冒充视频运动。截图与最终结果 JSON 存在 ignored `artifacts/native-previews/tv13-*.png`、`artifacts/tv13-playback-check.json`。

本轮没有证明外部订阅线路、直播连续播放、所有遥控器动作、音轨字幕和 ARMv7 实体设备均可用。核心界面可运行，设置等辅助页面仍保留较多上游布局，完整视觉统一见本地任务 04。

## 留存与复现

- 系统镜像：`${HOME}/.local/share/tv-android/sdk/system-images/android-33/android-tv/arm64-v8a`，约 4.1 GiB。
- 设备与数据：`${HOME}/.local/share/tv-android/avd/TV_1080p_API33.avd` 及同名 `.ini`，约 1.0 GiB。
- 继续使用先前独立 adb 端口 5039，避免与默认端口混用。不新增登录项或自动启动项。
- 本轮新增约 5.1 GiB，专用开发环境合计约 11 GiB；原 API 30 普通 Android 镜像保留作兼容性对照。
- 临时视频 HTTP 服务、端口转发、模拟器和 adb 在测试结束后停止；Swift 缓存、安装日志和自动化临时文件清理。AVD 与应用数据长期保留，便于再次启动。

其他机器需要先检查已有工具，然后通过已有 SDK 工具安装与创建设备：

```sh
sdkmanager 'system-images;android-33;android-tv;arm64-v8a'
ANDROID_AVD_HOME="$HOME/.local/share/tv-android/avd" avdmanager create avd \
  --name TV_1080p_API33 --package 'system-images;android-33;android-tv;arm64-v8a' --device tv_1080p
```

本地的启动脚本针对当前 macOS / Rosetta 环境，不是跨平台安装器。完整环境说明见 [ENVIRONMENT.md](ENVIRONMENT.md)。
