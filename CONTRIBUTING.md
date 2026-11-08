# 贡献指南

感谢你愿意为无限学堂贡献代码、文档或反馈！

## 提交问题（Issue）

- 缺陷报告请使用 **Bug report** 模板，尽量提供：应用版本、设备与 Android 版本、复现步骤、预期与实际结果、必要时的日志。
- 功能建议请使用 **Feature request** 模板，说明使用场景。
- 提问请先搜索已有 Issue 与 [docs](docs/)。

## 提交代码（Pull Request）

1. Fork 仓库，从 `main` 创建功能分支，例如 `feat/player-subtitles`、`fix/resume-position`。
2. 保持改动聚焦：一个 PR 解决一件事。
3. 在本地运行：
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
4. 如涉及业务规则，请为 `domain/` 或 `data/security/` 中的改动补充单元测试。
5. 如涉及用户可见的变化，请在 `CHANGELOG.md` 的 **Unreleased** 段落中添加一条记录。
6. 提交信息建议遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)，例如：
   - `feat: 增加字幕开关`
   - `fix: 修复拖动进度条后位置不保存`
   - `docs: 更新开发指南`

## 代码约定

- Kotlin 与 Compose 代码使用官方风格；公开函数尽量写中文注释说明"为什么"。
- 界面不直接访问数据库，统一通过 `data/repo` 中的仓库。
- 业务规则保持纯 Kotlin，不引用 Android 类型。
- 不要提交 `local.properties`、签名文件、构建产物或任何未授权的视频、图片与课件。
- 课程内容与视频需由贡献者拥有分发权利。

## 行为准则

请保持友善、尊重与建设性的讨论。骚扰、歧视与人身攻击将不被接受。
