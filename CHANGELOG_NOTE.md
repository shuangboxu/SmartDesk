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
