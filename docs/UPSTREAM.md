# 上游与设计来源

用户仓库：[tardlk/TV](https://github.com/tardlk/TV)，默认分支 `main`。本机 `origin` 指向用户仓库，`upstream` 指向下列 TVBoxOS 来源。

- 代码基线：[q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS)，提交 `ab11d289e09963a9daf65ca7f6b7a9a8cbe184e1`。
- 设计参考：[XiaochangXu/AVBox](https://github.com/XiaochangXu/AVBox)，用户已确认顶部导航 C 风格，并要求搜索键盘在左侧。
- 电视实现参考：[FongMi/TV](https://github.com/FongMi/TV)，按具体问题只读对照。
- 保留原有 `LICENSE` 与第三方许可。本工程的 applicationId 独立，宿主源码 namespace 与动态爬虫契约保持兼容。
- HTML 设计源、需求与截图保留在本工程 `.scratch/design/`。

上游遗留工作流仅以 `.github/workflows/test.yml.upstream-reference` 留作历史参考，不会执行；其中签名示例不适用于本项目。当前树移除了未使用的上游 `TVBoxOSC.jks`，保留历史与来源；TV 使用本机 Debug 密钥，正式签名尚未配置。
