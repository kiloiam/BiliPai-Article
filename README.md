# BiliPai 专栏

> 一个基于 BiliPai 网络层重构的**极简 B 站图文专栏阅读 Android app**。
> 只做一件事：**搜索 + 历史 + 净化阅读**。

## ✨ 特性

- 🎯 **专注专栏搜索**：B 站搜索结果只取 `search_type=article` 一支
- 📜 **搜索历史卡片化**：按"今天/昨天/更早"分组，长按删除，圆角 16dp
- 📚 **阅读历史卡片化**：底部「我的」tab，封面+标题+作者+「上次读到 N%」，长按删除
- 🌸 **B 站粉主题色** (`#FB7299`)，Material 3 dynamicColor 覆盖
- 🪟 **响应式布局**：
  - 手机竖屏：搜索框居中 → 顶部过渡动画
  - 手机横屏：左右分屏（搜索+历史 30% / 结果 70%）
  - 平板：双栏 Master-Detail
- ⚡ **原生 Compose 阅读**：调 `x/article/view` API + `LazyColumn` 渲染，**单页 30-60 MB**（vs WebView 200-635 MB）
- 🧠 **滚动位置记忆**：滚动停止 500ms 后落库，再次打开自动回到上次位置
- 🛡️ **无登录墙**：不依赖 cookie，公开接口即可读
- 🎚️ **字号调节**：阅读页 toolbar 可调 A− / A / A+
- 📤 **分享到系统**：调起原生分享面板
- 💾 **Room 存储历史**：Flow 驱动，UI 自动更新
- 🛡️ **WBI 签名** + buvid3 cookie：复制 BiliPai 已验证的反爬方案
- 📦 **APK 5-10 MB**（vs BiliPai 主线 30-40 MB，砍掉视频/直播/番剧/插件等 25+ 个重型依赖）

## 📷 截图

> 跑起来后自行体验。UI 设计原则：搜索框居中 → 输入时动画过渡到顶部。

## 🚀 快速开始

### 方式一：下载预编译 APK（推荐）

1. 打开本仓库的 [Actions](../../actions) 页面
2. 选择最新的 `Build Debug APK` workflow run
3. 滚到底部 **Artifacts** → 下载 `app-debug`
4. 在 Android 8.0+ 手机上安装

### 方式二：本地构建

#### 前置环境

| 工具 | 版本 |
|---|---|
| JDK | 21+ |
| Android SDK | API 35（compileSdk/targetSdk），API 26（minSdk） |
| Android Studio | 2024.1+（可选） |

#### 构建步骤

```bash
# 1. clone 仓库
git clone https://github.com/<your-name>/BiliPai-Article.git
cd BiliPai-Article

# 2. 生成本地 gradle wrapper（首次需要）
gradle wrapper --gradle-version 8.10.2

# 3. 构建 debug APK
./gradlew :app:assembleDebug

# 4. 安装到连接的设备
./gradlew :app:installDebug
```

APK 产物：`app/build/outputs/apk/debug/app-debug.apk`

#### 环境变量配置

Android Studio 用户：在 `local.properties` 中设置 SDK 路径：
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

命令行用户：设置 `ANDROID_HOME` 环境变量。

## 🏗️ 项目结构

```
BiliPai-Article/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── reader.css              # WebView 净化 CSS
│       ├── java/com/minipai/article/
│       │   ├── MainActivity.kt         # 入口 + NavHost
│       │   ├── ArticleApp.kt           # Application
│       │   ├── core/
│       │   │   ├── network/            # 网络层（从 BiliPai 抽取）
│       │   │   │   ├── WbiUtils.kt     # WBI 签名算法
│       │   │   │   ├── WbiKeyManager.kt # WBI 密钥缓存
│       │   │   │   ├── NetworkModule.kt
│       │   │   │   ├── SearchApi.kt    # searchArticle
│       │   │   │   ├── ArticleApi.kt   # getArticleView（备用）
│       │   │   │   ├── AppSessionCookieJar.kt # buvid3 自动生成
│       │   │   │   ├── FlexibleSerializers.kt
│       │   │   │   ├── TextUtils.kt
│       │   │   │   └── model/
│       │   │   │       ├── SearchModels.kt
│       │   │   │       └── NavModels.kt
│       │   │   ├── database/           # Room（单表）
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── SearchHistory.kt # entity + searchCount
│       │   │   │   └── SearchHistoryDao.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/          # B 站粉主题
│       │   │   │   ├── adaptive/       # WindowSizeClass
│       │   │   │   └── components/     # BiliSearchBar 等
│       │   │   └── util/
│       │   │       └── TimeGrouping.kt # 今天/昨天/更早
│       │   ├── data/
│       │   │   └── SearchRepository.kt # 含 signWithWbi
│       │   └── feature/
│       │       ├── search/             # 搜索页
│       │       │   ├── SearchScreen.kt
│       │       │   ├── SearchViewModel.kt
│       │       │   ├── SearchUiState.kt
│       │       │   ├── SearchHistoryPanel.kt
│       │       │   └── SearchResultList.kt
│       │       └── reader/             # 阅读页
│       │           ├── ArticleScreen.kt
│       │           └── ArticleWebView.kt
│       └── res/
│           ├── values/{strings,colors,themes}.xml
│           └── drawable/, mipmap-*/
├── gradle/libs.versions.toml           # 集中版本表
├── .github/workflows/build.yml         # CI：自动构建 APK
└── README.md
```

## 🎨 UI 设计原则

### 主题色

| 颜色 | 值 | 用途 |
|---|---|---|
| Primary | `#FB7299` | B 站粉，按钮/链接 |
| Primary container | `#FFE4ED` | 浅粉背景 |
| Secondary | `#00AEEC` | B 站蓝，正文链接 |
| Background | 跟随系统 | Material 3 dynamicColor |

### 动画

- 搜索框居中 → 顶部过渡：`animateDpAsState` + spring(stiffness = MediumLow)，~350ms
- 结果列表进入：`AnimatedVisibility` + `slideInVertically` + `fadeIn`
- 历史卡片删除：`animateItemPlacement` (LazyColumn)

### 自适应断点

| 窗口宽度 | 布局 |
|---|---|
| Compact (<600dp) | 单栏：搜索框居中 → 顶部 |
| Medium (600-840dp) | 左右分屏：搜索+历史 320dp / 结果自适应 |
| Expanded (≥840dp) | Master-Detail（同 Medium，未来可扩三栏） |

## 🧹 WebView 净化 CSS

阅读页通过 `assets/reader.css` 注入到 `https://www.bilibili.com/read/cv{id}`，隐藏：

- 顶栏（`.header`、`.bili-header`、`.nav-bar`）
- 侧栏（`.sidebar`、`.right-sidebar`、`.right-aux`）
- 底栏（`.bili-footer`、`.international-footer`）
- 登录弹窗、推荐列表、评论区、cookies 弹窗

并优化：

- 正文字号、行距、颜色（`#1a1a1a` on `#ffffff`）
- 最大宽度 720px 居中
- 引用块粉色左边框
- 代码块等宽字体
- 图片圆角 8dp

### 调试图文说明

B 站网页结构偶尔变动，CSS 选择器可能失效。调试步骤：

1. 用 Chrome 打开 `https://www.bilibili.com/read/cv123456`（任意 cv 号）
2. F12 → DevTools → Elements 面板
3. 右键 → Inspect Element 找到正文容器和 chrome 元素
4. 复制正确的 class 选择器更新 `assets/reader.css`
5. 重新构建 APK

## 🔌 与原 BiliPai 的关系

本项目**抽取**自开源项目 [BiliPai](https://github.com/jay3-yy/BiliPai) 的网络层和部分 UI 模式，重构成一个**只做专栏阅读**的极简 app。

| 维度 | BiliPai | BiliPai-Article |
|---|---|---|
| APK 体积 | 30-40 MB | 5-10 MB |
| 模块 | 视频/直播/番剧/动态/插件/下载 | 仅专栏搜索 + 阅读 |
| 依赖数 | 50+ | 17 |
| 阅读体验 | 内置 HTML 渲染 | WebView 净化（更简单） |
| 账号 | 扫码登录 | 无（不登录态） |

许可证：GPL-3.0（与 BiliPai 一致）。

## ❓ 常见问题

### Q: 搜索没结果？
A: B 站风控。未登录态高频搜索可能触发 -412 / -352。已在 `NetworkModule` 中注入 Chrome/131 UA + buvid3 + WBI 签名规避，但若仍被拦截，请稍等几分钟再试。

### Q: WebView 加载失败？
A: 阅读页有"在浏览器中打开"兜底按钮，可手动复制 URL 用系统浏览器读。

### Q: 想加登录？
A: 当前不登录态，登录后会多很多 UI（个人主页、关注、收藏）。可在未来扩展，但需要重新设计首屏。

### Q: 字号调节能持久化吗？
A: 当前每次启动重置为 17px。需要持久化可加 `DataStore` 一行代码。

## 📜 致谢

- [BiliPai](https://github.com/jay3-yy/BiliPai) — 网络层和搜索 UI 模式
- [B 站公开 API 文档](https://github.com/SocialSisterYi/bilibili-API-collect) — WBI 签名算法说明
- [Material 3](https://m3.material.io/) — 设计系统

## 📄 许可证

GPL-3.0 — 与原 BiliPai 一致。数据来源于 B 站公开接口，版权归对应权利人所有。本项目仅供学习交流。
