# P2：收口数据访问与页面状态

Category: enhancement
Status: ready-for-human
Blocked by: 01

依据：[主方案](../spec.md)。P2 初始接口与状态实现已验证，进入 P3 首页接入。

## 工作与验证

- [x] 按规格 §4 建立真实替换点上的 SourceAccess、LibraryStore 与 SettingsStore；初始 adapter 委派现有实现。
- [x] 引入 Kotlin ViewModel / 不可变 UiState；先覆盖首页所需请求，不一次重写完整 SourceViewModel。
- [x] 定义错误 / 空态 / 加载与请求归属，处理取消、换源和旧结果晚到；阻塞 JAR 继续遵守线程限制。
- [x] 旧 UI 保持可用，页面新增代码只通过统一访问层操作数据。
- [x] 通过公开接口验证数据与错误语义，防止同一请求有两条解析链。

## 完成条件

固定样本的成功 / 空 / 错误 / 取消 / 过期结果都有可复现验证，旧页面仍可使用；接口没有暴露源运行时细节。

## Comments

2026-10-12：用户要求制定保持现有 UI、借鉴 AVBox 演进方式的方案；当前仅规划。

2026-10-12：新增 Kotlin Content 模型、SourceAccess / LibraryStore / SettingsStore 及 HomeViewModel，现有 UI 尚未接入。8 项状态单测通过，真实配置 → 目录 / 搜索 / 并发搜索 / 详情 / 播放解析及收藏写入 2 项集成测试通过。搜索经原 EventBus 返回，已按 sourceKey + UUID 接入并在结束时注销。源调用复用原工作线程，观察者只在主线程注册 / 移除；不改变源协议。部分上游解析失败仍被旧层转为空结果，P8 拆加载模块时再明确语义，不虚构详细错误。
