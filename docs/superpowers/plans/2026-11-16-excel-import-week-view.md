# Excel Import and Week View Implementation Plan

**Goal:** 安卓手机离线导入现有 Excel，首次安装为空白课表，并可切换每日时间轴和每周总览。

**Architecture:** 保留 Compose/ViewModel/Repository/Room；新增纯 Kotlin XLSX 解析器、导入预览 ViewModel 和页面；导入事务替换及单份恢复备份；共享周次状态驱动日周页面。

**Tech Stack:** 现有 Kotlin、Compose、Room、JDK ZIP/XML、Android OpenDocument。

**Spec:** docs/superpowers/specs/2026-11-16-excel-import-week-view-design.md

## Global Constraints

- minSdk 26，离线，无 INTERNET 或 MANAGE_EXTERNAL_STORAGE 权限。
- applicationId 不变，versionCode 增加；旧数据保留，新安装空白。
- 用户选文件、预览、确认后才替换；失败不丢旧数据。

## Review Focus

- Excel 备注额外安排不得丢失；重复节次不可生成重复课程。
- 第6周调课以及周末课程正确；周总览跨节和重叠课程可见可点。
- 损坏ZIP、格式不匹配、过大文件拒绝且不改变数据库。
- 导入周次大于当前设置时阻止并提示，取消文件选择无副作用。
- 重复导入不会追加重复课程；恢复备份能够取回导入前数据。

### Task 1: 解析器和真实文件测试

- [x] 新建 `data/importing/TimetableImport.kt` 和 `XlsxTimetableParser.kt`，输出 `ImportedTimetable(courses: List<CourseDraft>, requiredWeeks: Int)`。
- [x] 在 `src/test/resources` 放入原始样例；`XlsxTimetableParserTest` 断言15门15安排、网络安全周六第6周、两位政策教师、备注图形学，拒绝损坏/不匹配/超限文件；先运行观察失败，再实现并运行。

### Task 2: 空白初始化、事务导入和恢复

- [x] `ScheduleRepository` 新增 `importTimetable(timetable)`、`restoreImportBackup()`、`observeImportBackupAvailable()`；`ScheduleStore` 对应实现。Room 保持 schema v1，备份JSON存入 app_meta，事务替换。
- [x] `ImportRepositoryTest` 和 Room 设备测试覆盖新安装空、旧数据保留、失败原子性、重复导入、备份恢复。原有seed测试保留为显式恢复原始数据的逻辑验证，新增首次初始化不灌入种子数据测试。
- [x] 先运行失败测试，再实现，运行 JVM 全套。

### Task 3: 手机导入流程和每周总览

- [x] `ui/importing/ImportViewModel.kt`、`ImportScreen.kt`：OpenDocument IO读取、完整预览、确认替换、可重试中文错误；从课表和设置可进入。
- [x] `ScheduleUiState` 添加视图模式和本周课程，`ScheduleViewModel` 订阅本周课程并保持日周选择；`WeekScheduleGrid.kt` 网格渲染跨节卡片，横向可滚动。
- [x] ViewModel 测试覆盖周切换、周六调课、导入取消/失败/成功；Compose 设备测试覆盖空态、切换及预览确认。

### Task 4: 构建和 Android 验证交付

- [x] 配置 Windows 可用 Java/SDK，执行 `gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
- [x] 使用 Android Emulator QA：设备发现、安装、实际Excel文件选择导入、日周视图切换、课程编辑、重启保留，检查截图与crash日志；执行设备测试。
- [x] 在项目主目录交付新版APK、简明手机使用说明及源码；记录真实验证结果。
