# P2：收口数据访问与页面状态

Category: enhancement
Status: ready-for-agent
Blocked by: 01

依据：[主方案](../spec.md)。P1 已通过，用户已授权本票；进行中。

## 工作与验证

- [ ] 按规格 §4 建立真实替换点上的 SourceAccess、LibraryStore 与 SettingsStore；初始 adapter 委派现有实现。
- [ ] 引入 Kotlin ViewModel / 不可变 UiState；先覆盖首页所需请求，不一次重写完整 SourceViewModel。
- [ ] 定义错误 / 空态 / 加载与请求归属，处理取消、换源和旧结果晚到；阻塞 JAR 继续遵守线程限制。
- [ ] 旧 UI 保持可用，页面新增代码只通过统一访问层操作数据。
- [ ] 通过公开接口验证数据与错误语义，防止同一请求有两条解析链。

## 完成条件

固定样本的成功 / 空 / 错误 / 取消 / 过期结果都有可复现验证，旧页面仍可使用；接口没有暴露源运行时细节。

## Comments

2026-10-12：用户要求制定保持现有 UI、借鉴 AVBox 演进方式的方案；当前仅规划。
