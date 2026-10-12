# P1：冻结基线并验证现代化构建链

Category: enhancement
Status: ready-for-agent
Blocked by: None

依据：[主方案](../spec.md)。用户已授权执行；本票进行中，按阶段出口推进。

## 工作与验证

- [ ] 补齐视觉 / 功能 / 遥控器基线，区分已验证与尚未知的外部线路、直播及 ARMv7 行为。
- [ ] 检查已有工具，选择 Kotlin / Compose / AGP / Gradle / Chaquopy 组合，锁定版本并记录理由；保留 Python 3.8 作为初始候选。
- [ ] 只加入构建能力，原页面、原播放器和 applicationId / minSdk 保持。
- [ ] 构建 Python32 / Python64，核验 ABI / JNI / 源引擎；用自有 JAR / JS / Python 样本确认执行，不把包内有库当作运行通过。
- [ ] 记录原生截图、操作路径、性能测量方法和最近通过代码点；工具路径、占用与清理写入环境文档。

## 完成条件

双 ABI Debug 与固定源样本验证通过，旧应用行为仍可复现；精确版本组合和基线有记录后进入 P2。

## Comments

2026-10-12：用户要求制定保持现有 UI、借鉴 AVBox 演进方式的方案；当前仅规划。

2026-10-12：用户明确授权“按照计划执行”。开始兼容构建链验证，先保持所有页面和播放器实现。
