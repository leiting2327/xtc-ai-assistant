# 小天才AI助手 ⌚💬

一款 Android 端 AI 助手 App，通过**无障碍服务**自动操作"小天才"家长端 App，将 AI 回复自动发送到手表微聊。

## 核心功能

- 🤖 **多 API 支持**：本地 Ollama（Qwen2.5-32B）+ Agnes AI（GPT-4o-mini）+ OC Studio 备用
- ⌚ **手表微聊发送**：通过无障碍服务自动打开小天才App、填入内容、点击发送
- 🔔 **接收手表回复**：通过通知监听服务捕获手表端回复
- 🔄 **主备API自动切换**：主API失败时自动切换到备用
- 🌐 **Web 控制面板**：在手机上设置 API、查看日志、管理对话

## 项目结构

```
XTCAIAssistant/
├── android/                  # Android Studio 项目
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/xtcai/assistant/
│   │   │   │   ├── MainActivity.java              # 主活动
│   │   │   │   ├── JsBridge.java                  # WebView↔原生桥
│   │   │   │   ├── XtcAccessibilityService.java   # 无障碍服务
│   │   │   │   ├── XtcNotificationService.java    # 通知监听
│   │   │   │   └── PreferenceManager.java         # 配置管理
│   │   │   ├── res/                                # 资源
│   │   │   └── assets/web/index.html               # Web界面
│   │   └── build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── web/                       # Web 端源码
│   └── index.html
├── .github/workflows/         # GitHub Actions 自动构建
│   └── build.yml
└── README.md
```

## 使用流程

### 1️⃣ 构建 APK
- 在 GitHub 上创建仓库
- 推送代码
- Actions 自动构建 APK，下载安装到手机

### 2️⃣ 权限设置（重要！）
首次运行需要：
- **无障碍服务**：设置 → 辅助功能 → 启用"小天才AI助手"
- **通知监听**：设置 → 通知 → 启用"小天才AI助手"

### 3️⃣ 配置 API
- 打开App → 设置
- 主 API（本地 Ollama）：`http://127.0.0.1:11434/v1`，模型 `qwen2.5:32b`
- 备用 API（OC Studio）：`https://guozi-ai.com/api/v1`，Key `gz-ai...`
- Agnes 备用：`https://api.agnes.ai/v1`，Key `sk-jKY...`

### 4️⃣ 使用对话
- 在对话页面输入问题
- AI 回答后自动打开小天才App、找到聊天、发送内容
- 手表端回复会通过通知监听显示在"手表回复"区域

## 注意事项

⚠️ **小天才App兼容性**：
- 包名 `com.xtc.watch` 已硬编码
- 如果小天才App更新了UI，资源ID会变化（已实现动态查找机制）
- 首次使用建议在"测试发送"功能中验证

⚠️ **隐私**：
- API Key 仅保存在本地SharedPreferences，不上传任何服务器
- 通知监听仅监听小天才App的通知

## 技术栈

- **前端**：HTML/CSS/JavaScript（纯原生）
- **原生**：Java + AndroidX
- **HTTP**：OkHttp 4.12.0
- **JSON**：Gson 2.10.1
- **构建**：Gradle 8.1.0 + Android Gradle Plugin
- **自动化**：GitHub Actions

## 本地构建

```bash
# 安装 Android Studio 后
cd android
./gradlew assembleDebug
# APK 输出：app/build/outputs/apk/debug/app-debug.apk
```
