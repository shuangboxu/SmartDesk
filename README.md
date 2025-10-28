# SmartDesk – 基于 JavaFX 的 AI 桌面助理

**课程项目：北京理工大学《Java语言程序设计》结课设计**  
**开发者：许双博（个人独立完成，AI辅助）**  
**周期：5周（2025年学期项目）**

---

## 🧭 一、项目概述

SmartDesk 是一款基于 **JavaFX** 的桌面级智能助理应用，集成了五个核心功能模块：
- 🗒️ **笔记管理**：支持创建、修改、搜索与导出笔记；
- 📅 **任务计划**：支持任务管理、优先级与定时提醒；
- 💬 **聊天助理**：可离线使用规则匹配，也可调用在线AI接口；
- 📄 **文档总结**：可导入TXT/PDF文档并生成摘要；
- ⚙️ **系统设置**：主题切换与AI模式配置。

项目旨在结合 Java 桌面开发与 AI 辅助设计思维，展示从界面、数据到算法的完整软件开发流程。

---

## 🎯 二、项目目标

1. 使用 JavaFX 实现一个模块化、可扩展的桌面应用；
2. 结合 SQLite 持久化与多线程任务机制；
3. 支持“离线 + 在线”双模式 AI 模块；
4. 展示工程化开发过程（Git + Maven 管理）；
5. 撰写完整文档与演示材料，体现真实的学习与思考过程。

---

## 🧱 三、系统架构

```
SmartDesk
│
├── ui/                    # JavaFX 前端
│   ├── MainApp.java       # 程序主入口
│   ├── controllers/       # 控制器
│   └── views/             # FXML 布局文件
│
├── core/                  # 业务逻辑层
│   ├── note/              # 笔记模块
│   ├── task/              # 任务模块
│   ├── chat/              # 聊天模块（AI 可插拔）
│   └── summary/           # 文档总结模块
│
├── storage/               # 数据存储层
│   ├── DatabaseManager.java
│   └── entity/            # 实体类
│
├── utils/                 # 工具类（网络、配置、时间等）
│
├── resources/             # 图标、CSS、配置文件
│
└── SmartDeskLauncher.java # 启动类
```

### 架构特征
- **MVC思想清晰**：UI 与业务逻辑分离；  
- **模块独立开发**：可单独测试与调试；  
- **可扩展性强**：支持未来插件式扩展（如邮件、语音模块）。

---

## 🧩 四、功能模块设计

| 模块 | 功能说明 | 关键技术 | 创新/亮点 |
|------|------------|------------|------------|
| **笔记管理** | CRUD + 搜索 + 导出 | JavaFX TextArea、SQLite、文件I/O | “AI总结”功能（离线/在线） |
| **任务计划** | 待办、优先级、提醒 | JavaFX ListView、多线程Timer | “智能排序”与提醒算法 |
| **聊天助理** | 对话窗口、AI接口 | HttpClient、JSON解析、规则匹配 | 多人格模式、可选AI接入 |
| **文档总结** | TXT/PDF分析、摘要 | PDFBox、文本分析算法 | 关键词提取、AI摘要 |
| **系统设置** | 主题切换、配置管理 | Properties 文件 | 离线/在线模式切换 |

---

## 🤖 五、AI模块设计（可插拔）

| 模式 | 启动条件 | 实现方式 | 示例 |
|------|------------|------------|------------|
| **离线模式** | 默认 | 本地规则算法 / 关键词匹配 | “时间”→系统当前时间 |
| **在线模式** | 配置文件中启用 | HTTP 调用 AI API | “总结笔记”→调用AI生成摘要 |

示例配置文件 `config.json`：

```json
{
  "useAI": true,
  "apiKey": "your_api_key_here",
  "model": "gpt-4o-mini"
}
```

> 无论联网与否，App 都具备完整演示功能。

---

## 🧠 六、数据库设计

| 表名 | 字段 | 说明 |
|------|------|------|
| `notes` | id, title, content, tag, date | 用户笔记 |
| `tasks` | id, title, priority, deadline, status | 任务信息 |
| `chat_history` | id, role, message, timestamp | 聊天记录 |

数据库采用 **SQLite** 本地持久化，使用 JDBC 管理。

---

## 🎨 七、界面设计（JavaFX）

- 顶部：应用标题栏（设置、退出）  
- 左侧：功能导航栏（笔记 / 任务 / 聊天 / 总结）  
- 右侧：功能区（TabPane布局）  
- 主题：浅色 / 深色（CSS切换）  
- 布局：FXML + Controller + CSS  

---

## ⚙️ 八、开发环境与技术栈

| 类别 | 工具 / 技术 |
|------|--------------|
| 开发语言 | Java 17 |
| 框架 | JavaFX |
| 数据库 | SQLite（JDBC连接） |
| 网络 | Java HttpClient / OkHttp |
| 库 | Gson、PDFBox、RichTextFX |
| IDE | IntelliJ IDEA |
| 版本控制 | Git + GitHub |
| AI辅助工具 | GitHub Copilot / ChatGPT Codex |

---

## 📅 九、5周开发计划

| 周次 | 工作内容 | 阶段成果 |
|------|------------|------------|
| 第1周 | 搭建项目架构、数据库连接、创建仓库 | 框架可运行，界面加载成功 |
| 第2周 | 实现笔记与任务模块 | 两模块独立可运行 |
| 第3周 | 聊天模块（离线模式） + 预留AI接口 | 聊天功能可演示 |
| 第4周 | 文档总结模块 + 美化UI | 所有模块整合完毕 |
| 第5周 | 测试、打包、编写文档与演示 | 最终成品提交展示 |

---

## 📘 十、AI辅助开发说明

本项目在开发过程中使用了 **GitHub Copilot** 与 **ChatGPT Codex** 进行代码生成与重构辅助。  
AI主要参与了：
- UI 控制逻辑与事件绑定；
- HTTP 请求与 JSON 解析；
- 模块接口的初步实现。

系统架构、数据库设计与主要算法由开发者独立完成。  
文档与注释均由本人手工编写，确保内容真实可信。  

---

## 🧾 十一、运行指南

```bash
# 克隆项目
git clone https://github.com/shuangboxu/SmartDesk.git

# 进入项目
cd SmartDesk

# 使用 Maven 构建
mvn clean javafx:run
```

---

## 🌱 十二、未来扩展方向
- 云端同步与账号系统  
- 语音识别 / 语音回复  
- 本地数据可视化仪表盘  
- 插件式功能扩展（反射机制加载）

---

## 💡 十三、项目优势总结
| 维度 | 展现能力 |
|------|-----------|
| 技术广度 | JavaFX、SQLite、网络编程、多线程、AI接口 |
| 工程规范 | 模块化架构、MVC模式、GitHub管理 |
| 创新性 | 可插拔AI模块、离线/在线双模式 |
| 展示性 | 完整GUI、交互流畅、界面美观 |
| 文档质量 | 手写反思、逻辑清晰、符合课程要求 |

---

**© 2025 许双博 | Beijing Institute of Technology**  
This project is created for educational purposes under the Java Programming Course.
