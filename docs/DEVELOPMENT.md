# 开发指南

## 环境

| 工具 | 版本 |
|------|------|
| JDK | 17 |
| Android SDK | platform 37.0（`platforms;android-37.0`）、build-tools 37.0.0 |
| Gradle | 随 Wrapper 下载（见 `gradle/wrapper/gradle-wrapper.properties`） |
| Android Studio | 建议使用近期稳定版 |

最低支持 Android 8.0（API 26），目标 API 36。

## 常用命令

```bash
./gradlew testDebugUnitTest      # 单元测试
./gradlew assembleDebug          # 调试 APK
./gradlew installDebug           # 安装到已连接的设备
./gradlew lint                   # 静态检查
```

调试 APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 本机配置

Android Studio 会自动生成 `local.properties`（记录 SDK 路径）。该文件已被 `.gitignore` 排除，**不要提交**。命令行环境可手动创建：

```properties
sdk.dir=/path/to/Android/Sdk
```

## 构建内存不足

Gradle 与 Kotlin 编译较吃内存。在内存较小的机器（例如 4 GB 内存且无交换分区的云主机）上，可以限制内存：

```bash
./gradlew -Dorg.gradle.jvmargs=-Xmx1200m -Dkotlin.daemon.jvmargs=-Xmx1024m assembleDebug
```

如仍被系统杀死，可增加交换分区。

## 修改课程内容

- 课程目录：`app/src/main/assets/courses.json`，格式见 [COURSES.md](COURSES.md)。
- 修改后重新安装应用即可生效（启动时重新载入目录）。
- 进度与笔记不会因目录替换而丢失，但请不要修改已发布课时的 `id`。

## 修改数据库结构

1. 修改 `data/db/Entities.kt` 或 `Daos.kt`。
2. 在 `AppDatabase.kt` 中提升 `version`。
3. 当前开发期使用 `fallbackToDestructiveMigration(true)`（重建数据库）。正式发布前，请为每次结构变化编写 `Migration` 并添加迁移测试。

## 发布

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 与 `versionName`。
2. 把 `CHANGELOG.md` 中 **Unreleased** 的内容移到新版本标题下，并更新底部的比较链接。
3. 提交并打标签：
   ```bash
   git tag v1.2.1
   git push origin v1.2.1
   ```
4. `release.yml` 工作流会自动构建、运行测试，并创建 GitHub Release，附带 APK 与对应的更新说明。

## 故障排查

| 现象 | 处理 |
|------|------|
| `sdkmanager` 找不到 `platforms;android-37` | 使用 `platforms;android-37.0` |
| 守护进程被杀（OOM） | 见上文"构建内存不足" |
| 播放器无法加载 `asset:///` 视频 | 确认文件位于 `app/src/main/assets/videos/` 且路径大小写一致 |
| 旋转后全屏状态异常 | 确认清单中 `MainActivity` 带有 `configChanges` |
