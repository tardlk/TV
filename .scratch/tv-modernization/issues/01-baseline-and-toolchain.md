# P1：冻结基线并验证现代化构建链

Category: enhancement
Status: ready-for-human
Blocked by: None

依据：[主方案](../spec.md)。用户已授权执行；本票构建与 ARM64 模拟器验证通过，ARMv7 真机验收保留到 P9。

## 工作与验证

- [x] 补齐视觉 / 功能 / 遥控器基线，区分已验证与尚未知的外部线路、直播及 ARMv7 行为。
- [x] 检查已有工具，选择 Kotlin / Compose / AGP / Gradle / Chaquopy 组合，锁定版本并记录理由；保留 Python 3.8 作为初始候选。
- [x] 只加入构建能力，原页面、原播放器和 applicationId / minSdk 保持。
- [x] 构建 Python32 / Python64，核验 ABI / JNI / 源引擎；用自有 JAR / JS / Python 样本确认执行，不把包内有库当作运行通过。
- [x] 记录原生截图、操作路径、性能测量方法和最近通过代码点；工具路径、占用与清理写入环境文档。

## 完成条件

双 ABI Debug 与固定源样本验证通过，旧应用行为仍可复现；精确版本组合和基线有记录后进入 P2。

## Comments

2026-10-12：用户要求制定保持现有 UI、借鉴 AVBox 演进方式的方案；当前仅规划。

2026-10-12：用户明确授权“按照计划执行”。开始兼容构建链验证，先保持所有页面和播放器实现。

2026-10-12 P1 结果：AGP 8.6.1 / Gradle 8.7 / Kotlin 2.0.21 / Compose BOM 2024.10.01 / TV Material 1.0.1 / Chaquopy 16.1.0 / Python 3.8 双 ABI 构建通过。原 JarLoader、JsSpider 与 PythonLoader 自有执行样本 3/3 通过。24 组页面比较中 21 组像素相同（排除时钟），3 组仅直播加载图标动画不同。详见 docs/MODERNIZATION.md。源码里的布局和播放器逻辑未替换，进入 P2。
