# Triage 标签

本地普通任务用 `Status:` 记录一个分诊状态；分类使用独立的 `Category:`，值为 `bug` 或 `enhancement`。

| Canonical role | 本仓库状态 | 含义 |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | 待维护者评估 |
| `needs-info` | `needs-info` | 等待补充信息 |
| `ready-for-agent` | `ready-for-agent` | 说明完整，可由 agent 接手 |
| `ready-for-human` | `ready-for-human` | 需要人工处理 |
| `wontfix` | `wontfix` | 不予处理 |

每个已分诊任务恰好使用一个分类和一个分诊状态。状态本身不扩大用户授权范围。

当 skill 要求使用某个 canonical role 时，写入表中对应字符串。Wayfinding 子票的领取和完成状态遵循 [issue-tracker.md](issue-tracker.md) 的 Wayfinding 约定。
