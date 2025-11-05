# SmartDesk Note Module Development Log

## 文件改动概览
- `src/main/java/com/smartdesk/core/note/NoteService.java`
- `src/main/java/com/smartdesk/storage/DatabaseManager.java`
- `src/main/java/com/smartdesk/storage/entity/NoteEntity.java`
- `CHANGELOG_NOTE.md`（当前文件）

## 主要改动说明
### `NoteService.java`
- 新增并实现笔记模块核心服务类，完成增删改查 CRUD 操作。
- 加入输入校验、日志记录以及数据库结果映射逻辑。

### `DatabaseManager.java`
- 实现 SQLite 数据库连接管理，包含驱动加载、连接获取以及数据表初始化。
- 增加 `notes` 表的建表 SQL 常量，启动时自动执行建表。

### `NoteEntity.java`
- 完成笔记实体类字段定义（id、title、content、tag、date）。
- 提供严格的空值校验、访问器与修改器方法，并覆盖 `toString` 便于调试。

---

## Task 模块拓展
- `src/main/java/com/smartdesk/core/task/TaskService.java`
  - 实现完整的任务 CRUD、筛选、仪表盘分组与提醒支持逻辑，并提供打卡、开始、延后等操作接口。
  - 适配 `ReminderScheduler` 的查询接口，自动维护提醒触发时间与任务状态。
- `src/main/java/com/smartdesk/core/task/model/Task.java`
  - 定义不可变领域模型，覆盖任务的时间、优先级、提醒、状态等字段并提供 Builder 便于 UI 使用。
- `src/main/java/com/smartdesk/core/task/model/TaskPriority.java`
  - 引入 5 级优先级体系并提供从数据库数值反解的帮助方法。
- `src/main/java/com/smartdesk/core/task/model/TaskType.java`、`TaskStatus.java`、`TaskLane.java`
  - 描述任务类型、生命周期状态及仪表盘分组枚举，方便 UI 与服务层共享语义。
- `src/main/java/com/smartdesk/core/task/model/TaskDashboardSnapshot.java`
  - 提供不可变的仪表盘投影对象，实现 Today/Upcoming/Someday 等分组的读取。
- `src/main/java/com/smartdesk/core/task/service/TaskQuerySupport.java`
  - 定义提醒调度所需的最小查询接口，解耦服务层与调度器。
- `src/main/java/com/smartdesk/core/task/scheduler/ReminderScheduler.java`
  - 基于单线程计划任务实现提醒扫描，支持动态调整扫描间隔与监听器回调。
- `src/main/java/com/smartdesk/storage/entity/TaskEntity.java`
  - 新增任务表对应实体，包含所有持久化字段及基础校验。
- `src/main/java/com/smartdesk/storage/DatabaseManager.java`
  - 扩展数据库初始化逻辑，新增 `tasks` 表及提醒查询索引。

## 任务面板适配
- `src/main/java/com/smartdesk/core/task/model/TaskLane.java`
  - 为各看板列补充中文标题、描述、配色与图标键值，方便前端直接消费展示元数据。
- `src/main/java/com/smartdesk/core/task/model/TaskBoardColumn.java`
  - 新增任务面板列视图模型，封装列元数据、任务列表以及完成率等统计指标。
- `src/main/java/com/smartdesk/core/task/model/TaskDashboardSnapshot.java`
  - 增加 `toBoardColumns`/`toBoardColumn` 转换方法，便于 UI 快速获取看板列数据。
- `src/main/java/com/smartdesk/core/task/TaskService.java`
  - 提供 `buildBoard` 与 `buildBoardLane` 便捷方法，直接输出面板列结构以供任务页面调用。

## 验证步骤
1. 执行 `mvn -DskipTests package` 编译项目，确保新增模块通过构建。
2. 在任意入口创建 `TaskService` 与 `ReminderScheduler`，调用 `createTask` 创建带提醒的任务，观察调度器回调是否触发。
3. 利用 `buildDashboard(LocalDate.now(), 3)` 获取仪表盘快照，确认任务被正确归类到 TODAY/UPCOMING/ANNIVERSARY 等分区。

## 最新调整 - 聊天配置与暗色主题
- 设置面板中的接口地址与模型输入改为可编辑下拉框，内置 OpenAI、DeepSeek 预设选项同时支持手动填写。
- 在线聊天请求会在缺省基础地址时自动补全 `/chat/completions`，并在地址缺失时给出明确提示。
- 优化暗色主题下的界面文字与输入控件对比度，使状态提示、标签、列表和输入框在暗色背景上更加清晰。

## 最新调整 - 聊天模块增强
- 聊天界面新增会话侧栏，可新建对话、在历史会话之间切换并显示最近更新时间与使用模型。
- 对话内容支持 Markdown 渲染，自动适配用户与 AI 的气泡颜色并优化代码块、引用、表格等排版。
- 发送区加入“插入资料”按钮，可从笔记或任务中挑选部分/全部字段一键插入给 AI。
- 会话内可快捷切换模型，所选模型会同步至当前会话并即时重建助手配置。
- 设置页支持维护自定义模型列表，并在保存时写入配置供对话界面选择。

## 最新调整 - 聊天文件上传
- `src/main/java/com/smartdesk/core/chat/ChatAttachment.java`
  - 新增二进制附件模型，支持从文件系统读取、展示描述信息以及转为 Base64 块传递给 AI。
- `src/main/java/com/smartdesk/core/chat/ChatMessage.java`
  - 扩展消息以携带附件列表，新增 `getPayloadContent` 将附件编码到请求文本中。
- `src/main/java/com/smartdesk/core/chat/ChatHistoryService.java` / `src/main/java/com/smartdesk/storage/DatabaseManager.java`
  - 构建 `chat_attachments` 表并在消息持久化/加载流程中读写附件 BLOB。
- `src/main/java/com/smartdesk/ui/chat/ChatView.java`
  - 发送区新增“上传文件”按钮、附件预览与“另存为”操作，支持无文本的纯附件消息。
- `src/main/java/com/smartdesk/core/chat/online/AbstractJsonAiClient.java`
  - 在线模型调用会携带附件展开后的 Base64 文本，确保 AI 可获取图片、文档内容。

## 最新调整 - 多提供方附件管线
- `src/main/java/com/smartdesk/core/chat/ChatAttachment.java`、`AttachmentStorage.java`
  - 附件仅保存文件名/类型/体积/路径等元数据，统一落盘后按需流式读取，完全移除 Base64 拼接。
- `src/main/java/com/smartdesk/core/chat/ChatHistoryService.java`、`DatabaseManager.java`
  - `chat_attachments` 表新增 `file_id` 列，持久化时写回附件 ID 并在加载时恢复临时文件。
- `src/main/java/com/smartdesk/core/chat/online/OpenAiClient.java`
  - 新增 OpenAI 客户端：先调用 `/v1/files` 上传附件获得 `file.id`，随后在 `/v1/responses` 请求中通过 `attachments.file_id` 引用。
- `src/main/java/com/smartdesk/core/chat/online/DeepSeekClient.java` 等
  - 统一通过 `AttachmentPromptFormatter` 将附件提取出的纯文本（或元信息摘要）拼接进 Prompt，避免再传输 Base64。
- `src/main/java/com/smartdesk/ui/chat/ChatView.java`
  - “另存为”操作直接从落盘路径复制原始文件，界面交互保持不变。
