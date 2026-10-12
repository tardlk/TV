# 搜索行为与 P4 迁移边界

2026-10-12 核对 `1a58dc23` 的 SearchActivity / FastSearchActivity、SearchKeyboard、布局、SearchHelper 与 SourceViewModel。普通搜索切片保留原生宿主，不修改键盘、播放器、快速搜索页面或默认搜索模式。

| 行为 | 普通搜索原行为与候选 | 快速搜索原行为（待迁移） |
| --- | --- | --- |
| 范围 | 仅 searchable，应用 SearchHelper 的指定源集合，首页源优先；无指定源显示原空态与提示 | 同样范围与首页源优先 |
| 匹配 | 去首尾空格精确匹配先展示；忽略空白后的标题前缀作为候补；没有精确结果且已完成至少 min(6, 总源数) 时，在接受正常响应的步骤提升候补 | 查询按空白分词，标题包含所有词才进入按源结果；主列表仍优先精确 / 前缀，按源列表展示分词匹配 |
| 并发 | 首次 HTTP 源立即发起，type 3 初始六个；3 秒推进后续任务；API 28 / 30 / 35 的阻塞 worker 上限分别为 12 / 18 / 24；单源 15 秒超时 | 所有源经 batch 调度，另有 2 秒 pump，3 秒 advance；同一 worker 上限与 15 秒超时 |
| 暂停 / 恢复 | 离开页面或打开详情 / 目录时暂停，保留已接受结果；返回只重启未完成源，以新代次拒绝旧响应 | 原实现打开详情 / 目录时暂停，返回重启未完成源；已有计数与按源筛选保持 |
| 目录 | `tag=folder` 经原 getList 协议加载；回退显示上一层列表。候选额外保留该层选中索引，原 TvRecyclerView 恢复焦点 | 记录目录列表、filterMode 与来源标识；换来源后目录返回退出页面 |
| action / 详情 | 保留原 Video 的 action / tag 和详情入口；普通搜索没有独立 action 执行分支，不新增协议。详情候补只保留同标题，按 sourceKey + id 去重，上限 20 | 保留原详情入口与同标题候补 |
| 遥控器 | 左侧原 SearchKeyboard、原确认 / 删除 / 远程入口、原结果列表；候选仅增加目录 / 详情返回的局部结果焦点恢复 | 原方向导航、海报放大、按源筛选与分词控件 |
| 输入 / 词表 | 原 IME、遥控器确认、ServerEvent 远程输入；SearchHelper / HistoryHelper 继续持有指定源和历史；原热词 / 拼音请求保持 | 搜索词 / 本地分词与分词事件保持 |
| 空 / 错误 | 空源、无匹配、全部超时或异常均沿用当前原生空态；新状态区分 Empty / Error，错误原因在旧解析层被归一为空时无法恢复 | 原状态栏展示源、结果、等待 / 超时数量 |

## 实现所有权

普通搜索由 `modern/search/SearchViewModel.kt` 持有不可变 StateFlow、调度、匹配、目录栈和详情候补。SearchActivity 仍拥有输入、词表、原生列表和一次详情导航。SourceAccess.folder 委派原 SourceViewModel.getList；搜索使用原 EventBus 解析桥，不新增协议实现。

原 LegacySourceAccess 将搜索放入共用的单线程 spThreadPool；页面接入前改为独立有界 search worker，避免阻塞详情 / 播放 / 目录。每次 HTTP 搜索使用 UUID 请求 tag，取消只作用于该请求，不调用全局 `cancelTag("search")` 或 `JsLoader.stopAll()`。协程取消会注销监听并中断 worker，但不能保证第三方 spider 的阻塞调用立即退出；未退出调用继续占用有界 worker，过期响应不得写回 UI。JS 内部的共用 HTTP tag 与运行时取消进一步收口留给 P8。

## 已知与后续

- 普通搜索的 Activity 配置重建保留 ViewModel 与结果，不重发已完成搜索；进程死亡恢复尚待后续验证。
- 原版目录返回与详情返回不恢复结果焦点；候选使用原 TvRecyclerView 的 selection 和 itemView.requestFocus 恢复，不引入全局焦点框架。
- 快速搜索状态、词表请求生命周期、完整空 / 错误与重建矩阵仍待迁移 / 验收；本切片不是 P4 完成。
- 外部订阅、ARMv7 真机、音频与完整播放生命周期仍按主方案待验收。
