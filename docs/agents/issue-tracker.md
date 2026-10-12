# Issue tracker：本地 Markdown

本仓库的任务和规格存放在 `.scratch/`。GitHub remote 仅表明源码上游，不是本项目的任务跟踪入口。

## 文件约定

- 每个工作主题一个目录：`.scratch/<feature-slug>/`。
- 规格：`.scratch/<feature-slug>/spec.md`。
- 实施任务：每票一个文件，路径为 `.scratch/<feature-slug>/issues/<NN>-<slug>.md`，从 `01` 开始编号。
- 普通任务在文件顶部附近写 `Status:`，分诊状态见 [triage-labels.md](triage-labels.md)。
- 依赖记录为顶部附近的 `Blocked by:` 行；编号只在当前主题目录内有效。
- 评论与会话记录追加到 `## Comments` 下。

## Skill 操作映射

- “publish to the issue tracker”：在对应主题目录内创建或更新 Markdown 文件。
- “fetch the relevant ticket”：读取明确引用的本地文件；仅给编号时先在当前主题内查找，存在歧义时确认主题。
- 创建或修改远程 Issue、PR 或评论需要用户另行明确授权。
- 配置任务跟踪不自动创建产品规格、拆分实施任务或开始实施；按用户当前授权推进。

## Wayfinding

Wayfinding 使用自己的执行状态，与普通任务的分诊状态分开解释：

- Map：`.scratch/<effort>/map.md`，包含 Notes / Decisions-so-far / Fog。
- Child：`.scratch/<effort>/issues/<NN>-<slug>.md`，每票一个文件，`Type:` 为 `research` / `prototype` / `grilling` / `task`。
- 子票 `Status:` 使用 `open` / `claimed` / `resolved`。
- `Blocked by: NN, NN` 所列子票全部 resolved 后才解除阻塞。
- Frontier：按编号选择首个 open、未阻塞、未领取的子票。
- Claim：开始该子票前保存 `Status: claimed`。
- Resolve：答案追加到 `## Answer`，保存 `Status: resolved`，并向 map 的 Decisions-so-far 追加摘要和链接。
