# P8：收敛存储和内部 Kotlin 实现

Category: enhancement
Status: ready-for-agent
Blocked by: 07

依据：[主方案](../spec.md)。本票属于方案拆分，尚未开始实施；按主方案的执行授权与阶段出口推进。

## 工作与验证

- [ ] 默认通过 SettingsStore 沿用 Hawk；按收益评估 SharedPreferences，MMKV 仅评估含 ARMv7 的 1.3.x 实际产物。
- [ ] 若替换后端，显式处理类型、缺省值与 TV 同包升级；保持单一写入后端，不迁移其他应用数据。
- [ ] 分类 / 列表 / 搜索 / 详情 / 播放加载逐个 Kotlin 化，每次保留原协议语义并通过 SourceAccess 验证。
- [ ] CatVod 动态契约层默认保留 Java；若个别文件必须迁移，用 javap / 反射和实际 JAR 对照公开签名。
- [ ] 只清理已完成替代与验证的旧入口、重复状态和临时 adapter，必要原生 View 与契约 Java 长期保留。

## 完成条件

设置 / 历史 / 收藏升级不丢失，JAR / JS / Python 执行与契约通过；无重复写入和引擎所有者，不以纯 Kotlin 比例替代功能验收。

## Comments

2026-10-12：用户要求制定保持现有 UI、借鉴 AVBox 演进方式的方案；当前仅规划。
