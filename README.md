# DayDayUp

DayDayUp 是一个基于 Jetpack Compose 开发的 Android 待办应用。它将任务分为 `Today` 和 `Plan` 两个时间范围，并提供提醒、壁纸、主题样式和回收站等功能。

## 功能

- 创建、编辑、完成和删除任务
- 将任务放入 `Today` 或 `Plan`
- 为 `Plan` 任务设置计划日期，到期后自动显示在 `Today`
- 为任务设置系统通知提醒
- 应用重启、设备重启或应用更新后恢复提醒
- 已删除任务进入回收站，保留 7 天后自动清理
- 使用内置壁纸或从相册选择自定义图片
- 调整壁纸模糊和压暗程度
- 切换柔和、粗野主义和手绘涂鸦风格
- 切换系统、衬线和等宽字体
- 自定义文字颜色和首页自我提醒

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX
- Kotlinx Serialization
- Gradle Version Catalog

## 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools 35
- Android 8.0（API 26）或更高版本的设备/模拟器

项目当前配置为：

| 项目 | 配置 |
| --- | --- |
| `compileSdk` | 35 |
| `targetSdk` | 35 |
| `minSdk` | 26 |
| `applicationId` | `com.doapp` |
| `versionName` | `1.0` |

## 使用 Android Studio 运行

1. 用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成。
3. 连接 Android 设备，或启动一个 API 26 以上的模拟器。
4. 选择 `app` 运行配置。
5. 点击 Run，或使用 Android Studio 的运行按钮安装应用。

首次打开提醒功能时，应用可能需要以下系统权限：

- 通知权限
- 精确闹钟权限
- 忽略电池优化限制

如果没有授予这些权限，提醒仍可能被系统延迟，或者完全无法显示。

## 命令行构建

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

macOS/Linux：

```bash
./gradlew assembleDebug
```

Debug APK 默认输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的 Android 设备：

```powershell
.\gradlew.bat installDebug
```

运行单元测试：

```powershell
.\gradlew.bat test
```

## 数据存储

应用数据保存在 Android 应用私有目录中，不会写入项目目录：

- 任务：`tasks.json`
- 外观设置：`SharedPreferences`
- 自定义壁纸：应用私有文件目录中的图片文件

卸载应用通常会同时清除这些本地数据。当前项目没有云同步功能。

## 目录结构

```text
.
├── app/
│   └── src/main/
│       ├── java/com/doapp/
│       │   ├── data/       # 任务和外观数据
│       │   ├── notify/     # 提醒、广播接收器和回收站清理
│       │   └── ui/         # Compose 页面、组件和主题
│       └── res/            # Android 资源
├── gradle/
│   ├── libs.versions.toml  # 依赖和插件版本
│   └── wrapper/            # Gradle Wrapper
├── tools/
│   └── emulator.ps1        # 当前电脑上的可选模拟器启动脚本
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## 上传到 GitHub

项目当前目录还不是 Git 仓库，可以在项目根目录执行：

```powershell
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<你的用户名>/<你的仓库名>.git
git push -u origin main
```

把命令中的仓库地址替换成你自己的 GitHub 仓库地址即可。

上传前可以检查将要提交的文件：

```powershell
git status
git ls-files
```

## 不建议上传的文件

以下内容已经由 `.gitignore` 排除，不需要手动提交：

- `local.properties`：包含本机 Android SDK 路径
- `.gradle/`、`.kotlin/`、`build/`：Gradle、Kotlin 和 Android 构建缓存/产物
- `.idea/`：Android Studio 的个人工作区配置
- `.codegraph/`：代码索引数据库和运行日志
- `.reasonix/`、`reasonix.toml`：本地工具数据和配置
- `*.apk`、`*.aab`、`mapping/`：构建输出
- `*.jks`、`*.keystore`、`keystore.properties`：应用签名材料
- `.env*`、`google-services.json`：可能包含密钥或环境配置

`tools/emulator.ps1` 可以保留，但它写死了当前电脑的模拟器名称和项目路径，不适合作为通用开发脚本。其他开发者应直接使用 Android Studio，或根据自己的模拟器配置修改该脚本。

## 许可证

当前项目尚未声明开源许可证。如果准备公开仓库，建议根据你的授权意愿补充 `LICENSE` 文件，例如 MIT、Apache-2.0 或 GPL-3.0。
