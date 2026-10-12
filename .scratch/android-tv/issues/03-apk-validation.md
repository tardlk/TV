# 03：APK 与设备验证

Category: enhancement
Status: ready-for-human
Blocked by: 02

- [x] Python32 / Python64 Debug 构建通过。
- [x] 包名、应用名、最低 API、ARMv7 / ARM64 独立 ABI 与 JS / Python 引擎核验。
- [x] APK 未嵌入用户测试订阅或站点配置。
- [x] 订阅目录、列表、详情协议抽查：三个通过，一个返回非 JSON。
- [x] 模拟器初始空壳、配置导入、JAR 首页与采集源详情走查。
- [x] 完整 Android 11 ARM64 模拟器中固定视频预览 / 全屏出画面、运动及控制层截图检查。
- [ ] ARMv7 实体电视验证（当前未连接对应设备）。

## Comments

2026-10-12：通过 `scripts/verify-apk.py` 核验 Android 9+、独立身份和纯 ABI 包；报告在 `artifacts/validation-armv7.json` 与 `validation-arm64.json`。ARM64 模拟器结果不代替 ARMv7 真机结果。

2026-10-12：最终两个 APK 的 v2 签名校验通过；实际外部线路仍未确认播通，ARMv7 真机与完整遥控器操作验收待完成。自动截图工具等待 idle 超时不等于应用崩溃；本轮 AndroidRuntime 日志无 fatal。固定样本暴露的空 lives 数组解析问题已用最小保护修复并复验。

2026-10-12 补测：官方 Android TV 13 / API 33 ARM64 1080p AVD 配置并保留。顶部方向导航、搜索 / 设置、固定样本详情 / 全屏、媒体键暂停恢复、历史和用户订阅首页通过；完整记录见 docs/TV_EMULATOR.md。ARMv7 真机与外部线路 / 直播仍未验收。
