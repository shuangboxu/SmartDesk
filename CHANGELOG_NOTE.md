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

## 测试与验证
1. 运行 `mvn -q -DskipTests package` 构建项目（需联网以下载依赖）。
2. 在任意 JavaFX 入口（如 `MainApp`）中创建 `DatabaseManager` 与 `NoteService` 实例，调用 CRUD 方法验证：
   - `createNote`：插入新笔记并确认返回对象包含自增 ID；
   - `getNoteById` / `getAllNotes`：查询并确认返回结果；
   - `updateNote`：更新标题或内容后再次查询验证；
   - `deleteNote`：删除后确认查询结果为空。
3. 若执行过程中出现数据库文件权限问题，可删除根目录下的 `smartdesk.db` 重新执行。

## 遇到的问题
- 首次构建可能出现依赖仓库连接失败（HTTP 403 或网络限制），可稍后重试或手动配置国内镜像。

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

## 验证步骤
1. 执行 `mvn -DskipTests package` 编译项目，确保新增模块通过构建。
2. 在任意入口创建 `TaskService` 与 `ReminderScheduler`，调用 `createTask` 创建带提醒的任务，观察调度器回调是否触发。
3. 利用 `buildDashboard(LocalDate.now(), 3)` 获取仪表盘快照，确认任务被正确归类到 TODAY/UPCOMING/ANNIVERSARY 等分区。
