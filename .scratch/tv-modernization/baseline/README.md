# 原生界面候选基线

主要来自已交付 0.1.0 Debug 的 Android TV 13 / API 33 ARM64、1920×1080 截图；播放控制层另采用此前 Android 11 ARM64、1280×720 的固定样本截图。主代码对应 a2f5494，环境与测试记录对应 d1cf993。这里只复制现有测试图片，没有重新运行应用或修改画面。

| 页面 | 截图 |
| --- | --- |
| 首次使用 | [empty-home.png](empty-home.png) |
| 固定样本首页 | [home.png](home.png) |
| 固定样本详情 | [detail.png](detail.png) |
| 搜索 / 左侧键盘 | [search.png](search.png) |
| 全屏 / 暂停 | [player.png](player.png) |
| 播放控制层（720p / Android 11） | [player-controls-720p.png](player-controls-720p.png) |
| 历史 | [history.png](history.png) |
| 设置 | [settings.png](settings.png) |

内容均为自制样本、空态或设置；不包含用户订阅地址与第三方影视海报。当前主要覆盖已有深色、1080p 画面及一张 720p 控制层画面，尚不构成完整验收集。P1 需补齐浅色、720p、收藏、直播与相关错误 / 加载 / 空态，并记录每张图的配置和操作路径。

“保持 UI”以当前原生画面为依据。HTML 是设计参考，不自动覆盖当前应用；未完成的辅助页面视觉统一应单独评审。截图中的系统时钟、视频位置和测试文案变化，不属于重新设计。
