# ColorOS Feature Enhance 可视化编辑器

一个用于 **可视化编辑与管理 ColorOS 特性开关**（如 `com.oplus.app-features.xml` 与 `com.oplus.oplus-feature.xml`）的开源工具，使用 **Kotlin + Jetpack Compose** 开发，遵循 **Material 3 Expressive** 设计规范。

> ⚠️ 本应用 **需要 Root 权限** 才能正常读写系统配置文件，请确保目标设备已 Root。

---

## ✨ 主要功能

- **双模式编辑**：一键在 *App-Features* 与 *Oplus-Features* 模式之间切换；
- **实时可视化**：按照「描述 → 分组 → 开关」层级展现所有特性，所见即所得；
- **搜索 & 高亮**：支持按名称 / 描述高速模糊搜索并自动高亮匹配关键字；
- **分组折叠**：同一描述的特性自动归为同组，可展开 / 折叠查看；
- **快速增删改**：长按列表项可删除，悬浮按钮（FAB）可新增，开关即改值；
- **文本编辑模式**：内置纯文本编辑器，可直接查看 / 编辑原始 XML；
- **国际化**：内置简体中文、英语双语；
- **数据持久化**：用户自定义描述映射持久保存于 `SharedPreferences`；
- **示例配置**：仓库 `exampleConfig/` 提供参考 XML，方便快速体验。

---

## 📂 项目目录

```text
com.itosfish.colorfeatureenhance
├── data
│   ├── model/                 # 数据模型（AppFeature、FeatureGroup 等）
│   └── repository/            # XML 读写仓库（XmlFeatureRepository…）
├── domain
│   └── FeatureRepository.kt   # 领域层接口，抽象数据操作
├── navigation
│   └── Navigation.kt          # 应用导航路由
├── ui
│   ├── components/            # 可复用 UI 组件（SearchBar、FAB…）
│   ├── screens/               # 完整页面（未来扩展）
│   ├── search/                # 搜索逻辑（SearchLogic）
│   ├── theme/                 # Material 3 Expressive 主题定义
│   ├── FeatureConfigScreen.kt # 主要配置界面
│   └── TextEditorActivity.kt  # 内置文本编辑器
├── utils                      # 工具类（ConfigUtils、DialogUtil…）
├── FeatureMode.kt             # 模式枚举
└── MainActivity.kt            # 应用入口
```

---

## 🏗️ 架构说明

| Layer | 关键职责 | 代表文件 / 目录 |
|-------|-----------|----------------|
| UI    | Compose 渲染、交互、导航 | `ui/` `Navigation.kt` |
| Domain| 业务规则、接口定义        | `domain/FeatureRepository.kt` |
| Data  | 数据获取、XML 解析 & 持久化 | `data/` `utils/ConfigUtils.kt` |

- **协程**：所有 I/O 均通过 `Dispatchers.IO` 调度，避免阻塞主线程。
- **依赖倒置**：UI 仅依赖接口，不直接依赖数据实现，方便测试与扩展。

---

## 🚀 编译与运行

1. **环境要求**
   - Android Studio **Giraffe / Hedgehog** 及以上；
   - Android Gradle Plugin 8+；
   - Kotlin 1.9+；
2. **克隆仓库**
   ```bash
   git clone https://github.com/yourname/ColorFeatureEnhance.git
   ```
3. **导入项目**：使用 Android Studio 打开根目录，等待 Gradle 同步完成；
4. **连接 Root 设备** 并启用 USB 调试；
5. **Run ▶️**：选择目标设备后点击运行；
6. **准备配置文件**（可选）：将 `com.oplus.*.xml` 复制到
   `/sdcard/Android/data/com.itosfish.colorfeatureenhance/files/`；

> 亦可直接使用 `exampleConfig/` 中的示例 XML 体验应用功能。

---

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **异步**：Kotlin Coroutines
- **持久化**：SharedPreferences
- **其他**：XML Pull 解析、Android ViewModel (如需)

---

## 🤝 贡献指南

欢迎提 Issue、PR 或提出功能建议！提 PR 前请确保：

1. 遵循 `ktlint` / `detekt` 代码规范；
2. 所有新特性附带中 / 英文注释；
3. 不破坏现有功能，任何跨模块改动需同步更新文档；
4. 通过 CI / 本地单元测试。

---

## 📜 许可证

Apache-2.0 © 2024 itosfish & Contributors

---

> “Make editing features as simple as flipping a switch.”
