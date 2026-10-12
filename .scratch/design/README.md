# TV HTML 视觉原型：设计评审

这是一次性 HTML 视觉原型。用户已于 2026-10-12 选定 C 为首页布局，HTML 默认打开 C；详情、播放、搜索、历史、收藏、设置与直播已串联，这是设计阶段档案；当前 Android 实现进度见 `../android-tv/spec.md`。

## 打开

直接用浏览器打开 [prototype-top-nav.html](prototype-top-nav.html)，无须安装依赖或运行服务器。macOS 在仓库根目录也可执行：

```sh
open .scratch/design/prototype-top-nav.html
```

底部深色栏是设计评审工具，不属于 TV 应用界面。它支持切换页面、浅深色和首次使用空态；首页仍可查看 A / B 留档。“纯画面”在新标签页打开无评审工具栏的版本。

## 连贯预览

从首页点「查看详情」或继续观看卡片 → 在详情点播放入口或某一集 → 打开全屏播放控制层。播放层可切集、拖动示例进度、切线路、倍速、字幕与音轨，返回详情时保持本次预览状态。

顶部导航可进入直播、历史、收藏与设置，右侧搜索按钮进入搜索页。底部评审栏也可直接打开这些页面。

| 页面 / 面板 | 截图 |
| --- | --- |
| 影片详情 | [浅色](previews/detail-light.png) / [深色](previews/detail-dark.png) |
| 全屏播放控制层 | [预览](previews/player-controls.png) |
| 选集与更多参数 | [选集](previews/player-episodes.png) / [更多](previews/player-more.png) |
| 搜索 | [输入页](previews/search-light.png) / [结果页](previews/search-results.png) |
| 历史 | [预览](previews/history-light.png) |
| 收藏 | [预览](previews/collection-light.png) |
| 设置 | [预览](previews/settings-light.png) |
| 直播 | [预览](previews/live-light.png) |

## 比较内容

| 布局 | 重点 | 预览 |
| --- | --- | --- |
| A · 推荐大卡 | 接近 AVBox 首页的推荐大卡与竖版海报行 | [浅色](previews/home-A-light.png) / [深色](previews/home-A-dark.png) |
| B · 分类片库 | 更高内容密度，分类、筛选与双排海报 | [预览](previews/home-B-light.png) |
| C · 继续观看（已选定） | 主推荐与观看记录形成两层结构 | [预览](previews/home-C-light.png) |
| 首次使用 | 不含站点或影片的空壳引导 | [预览](previews/empty-light.png) |

URL 参数：`page=home` / `detail` / `player` / `search` / `history` / `collection` / `settings` / `live`；`variant=A` / `B` / `C`，`theme=light` / `dark`，`state=demo` / `empty`，`clean=1` 隐藏评审工具。可在首页非输入状态用左右键切换留档布局，Esc 关闭面板或从播放返回详情；这不是 Android 遥控器实现。

所有片名、频道、节目、记录与图形海报都是视觉样例。未接入真实接口、外部图片或字体，也不会保存配置。播放暂停、切换线路等只改变示例 UI 状态，不播放视频、不扫描设备、不读取用户文件。首次安装空壳状态不展示这些示例内容。

## 检查记录

2026-10-12，复用本机已有 Node.js、Playwright 和 Microsoft Edge，无新增安装。

- 1920×1080 下检查 A / B / C、深色 A 和首次使用空态截图。
- 1280×720 下检查带评审栏的预览缩放，无水平溢出。
- 布局切换会更新 URL；主题和空态切换可用。
- 输入框内左右键不会切换布局，Esc 可关闭预览弹窗。
- 检查期间无脚本异常，无 HTTP / HTTPS 请求。
- 空态没有示例影片卡片。

点播主流程与其他页面的补充检查：

- 1920×1080 下检查详情浅深色、播放层、选集和更多面板，以及搜索、历史、收藏、设置、直播截图。
- 1280×720 下完整评审栏可见，无水平溢出。
- 首页 / 搜索 / 历史 / 收藏可进入详情，选集可进入播放；返回和浏览器后退可用。
- 播放暂停、上一集 / 下一集、进度、线路、倍速、字幕、音轨和更多参数更新示例状态；换线路保持当前选集和进度。
- 搜索输入作为文本显示，样例搜索可返回结果，未匹配时显示无结果。
- 历史清空、收藏管理删除、设置主题与播放器选择、直播切换频道和收起列表可预览。
- 搜索、历史、收藏、直播的空态均无示例影片或频道。
- 上述检查未发现脚本异常或 HTTP / HTTPS 请求；不是 Android 端功能测试。

保留源码、需求记录、任务与截图约 4.2 MiB，用于后续评审。本次浏览器检查使用独立临时会话，结束后已关闭；没有运行中的服务器、后台服务、自动启动项或新增开发环境。Playwright 的临时浏览器会话目录由浏览器关闭流程清理。

## 设计结论

首页采用 C：顶部主导航，大图与片名详情并列，下方继续观看。A / B 仅保留为比较资料。用户原话及确认记录见 [首页任务](issues/01-top-nav-prototype.md)。其余 HTML 已完成，等待整体视觉评审，见 [点播主流程](issues/02-detail-player-prototype.md) 与 [其他页面](issues/03-supporting-pages-prototype.md)。Android 原生实现与验证另见 `../android-tv/spec.md`。
