# 01：构建环境与上游基线

Category: enhancement
Status: ready-for-human
Resolution: verified
Blocked by: None

- [x] 复用已有工具，记录专用环境与安装校验。
- [x] 替换上游 Python 本机路径，保持所有引擎。
- [x] 完成 ARMv7 Debug 基线构建或记录准确阻塞。
- [x] 测试订阅保存在忽略目录，不进入默认资源。

## Comments

2026-10-12：用户明确授权开工；不重复询问环境安装或本地实现许可。

2026-10-12：上游 Java32 Debug 基线构建通过；仅替换 Room 注解处理器的宿主 SQLite JDBC，并使用可配置的 Python 路径。最终 Python32 / Python64 Debug 均已构建通过，匹配 Python 3.8 后不再出现字节码编译版本警告。
