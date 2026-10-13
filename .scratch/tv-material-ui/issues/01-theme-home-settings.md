# Material 3 for TV：主题、首页与设置样板

Category: enhancement
Status: ready-for-human

依据：[规格](../spec.md)。复用现有环境与已通过的 Compose / TV Material 依赖，保留 C 首页和设置全部能力。

## 工作与验证

- [x] 完整浅 / 深色主题、字体和形状；共用胶囊与设置行。
- [x] 首页统一导航和推荐区视觉，原生分类 / 历史宿主保留。
- [x] 设置显示层采用 Compose；每个旧入口 / 可见性 / 值 / 动作有映射。
- [x] 遥控器、弹窗、设置落盘 / 重建、源与播放回归通过。
- [x] 双 ABI Debug、包检查、四组截图与性能对照完成。
- [x] 文档 / 环境清理与本地提交完成。

## Comments

2026-10-13：按用户选择开始第一组视觉升级。旧版 0.2.2-dev 为对照；原生播放器与复杂源处理继续复用。

2026-10-13：0.3.0-dev（versionCode 5）完成样板。TvTheme 提供暖色语义配色、字体层级和形状；首页导航 / 推荐区使用新主题，C 结构与图片加载器保持。设置 29 个原动作分为四组，值通过 TvSettingValue 驱动 Compose，原 Java 协调者继续拥有弹窗、Hawk 与联动；没有新增设置后端。未使用的 fragment_model.xml 暂留作旧 ID 声明与对照，运行入口仅使用 fragment_material_settings.xml。

移除旧单页 ViewPager 宿主，改用 FragmentContainerView，避免 Compose 长列表焦点使整个窗口平移。单项 500ms 确认防重复不禁用按钮；列表滚动时标题和类别栏保持固定。配置 / 播放器 / 搜索展示弹窗及返回、开关联动、值显示、Activity 重建通过设备测试。

双 ABI Debug、34 项单测、13 项最终设备测试通过；四组 720p / 1080p、浅 / 深色分别运行空壳截图、固定源播放及全部设置遍历，共 16 次矩阵测试执行。证据在 ignored artifacts/material-ui；包检查和 v2 签名通过。性能与环境结果见 docs/MODERNIZATION.md、docs/ENVIRONMENT.md。当前状态表示样板待用户审阅；P4 快速搜索、P5 设置完整状态迁移与直播仍未完成。
