# 我的课表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一款预置 2026–2027 学年第一学期课程、支持离线查看与增删改，并交付可直接安装 Debug APK 的原生安卓应用。

**Architecture:** 单 Activity Compose 应用使用 UI → ViewModel → Repository → Room 的单向数据流。课程与上课安排一对多建模，纯 Kotlin 领域函数负责教学周、周次位掩码、校验和冲突检测，Room 只负责持久化。

**Tech Stack:** Kotlin 2.3.21、Android Gradle Plugin 9.4.0、Gradle 9.6.0、Jetpack Compose BOM 2026.08.00、Material 3、Room 2.8.4、Lifecycle 2.11.0、Navigation Compose 2.9.8、KSP 2.3.11、JUnit 4。

**Spec:** `docs/superpowers/specs/2026-09-07-android-course-schedule-design.md`

## Global Constraints

- 应用名称：`我的课表`。
- applicationId 与 namespace：`com.songkeyuan.schedule`。
- `minSdk = 26`，`targetSdk = 36`，`compileSdk = 36`。
- 第 1 周周一默认为 `2026-08-31`，总教学周数默认为 16，设置范围为 1–30。
- APK 不声明 `android.permission.INTERNET`，不实现账号、云同步、通知或运行时 Excel 导入。
- 首次数据必须包含 12 门已安排课程、3 门待安排课程和 14 条上课安排。
- 所有业务行为先写失败测试并确认失败原因，再写最小实现；配置文件、资源和 Gradle Wrapper 属于无行为脚手架。
- 每个任务完成后运行该任务测试；最终必须运行全部单元测试、Lint 和 Debug APK 构建。
- 使用当前官方稳定依赖，不使用 alpha、beta 或动态版本。版本依据：[AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes)、[Compose BOM](https://developer.android.com/develop/ui/compose/bom)、[AndroidX 稳定版本](https://developer.android.com/jetpack/androidx/versions)。

---

## File Map

- `settings.gradle.kts`：仓库与模块声明。
- `build.gradle.kts`：项目级插件声明。
- `gradle/libs.versions.toml`：固定插件和依赖版本。
- `gradle/wrapper/*`、`gradlew*`：Gradle 9.6.0 Wrapper。
- `app/build.gradle.kts`：Android、Compose、Room、测试配置。
- `app/src/main/AndroidManifest.xml`：离线应用清单。
- `app/src/main/java/com/songkeyuan/schedule/domain/*`：日期、周次、校验与冲突纯逻辑。
- `app/src/main/java/com/songkeyuan/schedule/data/*`：Room 实体、DAO、数据库、初始课表与 Repository。
- `app/src/main/java/com/songkeyuan/schedule/ui/*`：Compose 页面、导航、主题与 ViewModel。
- `app/src/test/*`：可在 JVM 运行的领域、Repository、ViewModel 测试。
- `app/src/androidTest/*`：Room 与 Compose 设备测试。
- `README.md`：安装、构建、数据和兼容性说明。

---

### Task 1: Android 工程脚手架与教学周计算

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/WeekCalculator.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/PeriodTime.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/domain/WeekCalculatorTest.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/domain/PeriodTimeTest.kt`

**Interfaces:**
- Produces: `WeekCalculator.weekFor(date: LocalDate, firstWeekMonday: LocalDate, totalWeeks: Int): Int`
- Produces: `WeekCalculator.mondayOf(week: Int, firstWeekMonday: LocalDate): LocalDate`
- Produces: `PeriodTime.forPeriod(period: Int): ClosedRange<LocalTime>`

- [ ] **Step 1: Install only the Android build prerequisites missing from the machine**

Use the official macOS ARM command-line tools package `commandlinetools-mac_arm64-15859902_latest.zip` and checksum `835b62a26162b229b441d1f6d4680383815a270809eb33522c0d480fa5002c4e` if `sdkmanager` is absent. Install `platform-tools`, `platforms;android-36`, and `build-tools;36.0.0` into `/Users/sky/Library/Android/sdk`. Accept the Android SDK license only through `sdkmanager --licenses`. Keep `local.properties` untracked with:

```bash
curl -fL https://dl.google.com/android/repository/commandlinetools-mac_arm64-15859902_latest.zip -o /tmp/commandlinetools-mac_arm64-15859902_latest.zip
shasum -a 256 /tmp/commandlinetools-mac_arm64-15859902_latest.zip
unzip /tmp/commandlinetools-mac_arm64-15859902_latest.zip -d /tmp/android-command-line-tools
mkdir -p /Users/sky/Library/Android/sdk/cmdline-tools/latest
cp -R /tmp/android-command-line-tools/cmdline-tools/. /Users/sky/Library/Android/sdk/cmdline-tools/latest/
/Users/sky/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/Users/sky/Library/Android/sdk --licenses
/Users/sky/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/Users/sky/Library/Android/sdk "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

Verify the printed SHA-256 exactly matches the expected checksum before extracting the archive.

```properties
sdk.dir=/Users/sky/Library/Android/sdk
```

Use the installed Java 26 runtime with Gradle 9.6.0; Gradle 9.4.0 and newer support running on Java 26.

- [ ] **Step 2: Create the Gradle and Android configuration**

Pin these versions in `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.4.0"
kotlin = "2.3.21"
ksp = "2.3.11"
composeBom = "2026.08.00"
activityCompose = "1.13.0"
coreKtx = "1.19.0"
lifecycle = "2.11.0"
navigation = "2.9.8"
room = "2.8.4"
coroutines = "1.10.2"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Configure `app/build.gradle.kts` with built-in Kotlin from AGP 9.4, Compose enabled, Java 17 bytecode, Room schema export to `$projectDir/schemas`, and the dependencies above. Do not apply `org.jetbrains.kotlin.android`; AGP 9.x supplies built-in Kotlin.

Create the wrapper with a temporary verified Gradle 9.6.0 binary distribution, then use only `./gradlew` for project commands:

```bash
/tmp/gradle-9.6.0/bin/gradle wrapper --gradle-version 9.6.0 --distribution-type bin
./gradlew --version
```

- [ ] **Step 3: Write failing teaching-week tests**

```kotlin
class WeekCalculatorTest {
    private val firstMonday = LocalDate.of(2026, 8, 31)

    @Test fun firstMonday_isWeekOne() {
        assertEquals(1, WeekCalculator.weekFor(firstMonday, firstMonday, 16))
    }

    @Test fun followingMonday_isWeekTwo() {
        assertEquals(2, WeekCalculator.weekFor(LocalDate.of(2026, 9, 7), firstMonday, 16))
    }

    @Test fun datesOutsideSemester_areClamped() {
        assertEquals(1, WeekCalculator.weekFor(LocalDate.of(2026, 8, 1), firstMonday, 16))
        assertEquals(16, WeekCalculator.weekFor(LocalDate.of(2027, 2, 1), firstMonday, 16))
    }

    @Test fun weekMonday_usesSemesterAnchor() {
        assertEquals(LocalDate.of(2026, 9, 7), WeekCalculator.mondayOf(2, firstMonday))
    }
}
```

Add `PeriodTimeTest` with literal assertions for periods 1, 5, 6, 9, 10, and 12, including the midday and evening gaps.

- [ ] **Step 4: Run tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*WeekCalculatorTest' --tests '*PeriodTimeTest'
```

Expected: compilation fails because `WeekCalculator` and `PeriodTime` do not exist.

- [ ] **Step 5: Implement the minimal date and period behavior**

```kotlin
object WeekCalculator {
    fun weekFor(date: LocalDate, firstWeekMonday: LocalDate, totalWeeks: Int): Int {
        require(totalWeeks in 1..30)
        val rawWeek = ChronoUnit.WEEKS.between(firstWeekMonday, date).toInt() + 1
        return rawWeek.coerceIn(1, totalWeeks)
    }

    fun mondayOf(week: Int, firstWeekMonday: LocalDate): LocalDate {
        require(week >= 1)
        return firstWeekMonday.plusWeeks((week - 1).toLong())
    }
}
```

Implement `PeriodTime` as an immutable map for all 12 periods using the exact times in the specification, and reject values outside 1–12 with `IllegalArgumentException`.

- [ ] **Step 6: Run tests and verify GREEN**

Run the Task 1 test command again. Expected: all Task 1 tests pass.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle app
git commit -m "build: scaffold Android course schedule app"
```

---

### Task 2: 周次位掩码、安排校验与冲突检测

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/WeekMask.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/MeetingDraft.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/MeetingValidator.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/domain/ConflictDetector.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/domain/WeekMaskTest.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/domain/MeetingValidatorTest.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/domain/ConflictDetectorTest.kt`

**Interfaces:**
- Produces: `WeekMask.fromWeeks(weeks: Set<Int>, totalWeeks: Int): Long`
- Produces: `WeekMask.toWeeks(mask: Long, totalWeeks: Int): Set<Int>`
- Produces: `WeekMask.format(mask: Long, totalWeeks: Int): String`
- Produces: `MeetingValidator.validate(candidate: MeetingDraft, totalWeeks: Int): List<String>`
- Produces: `ConflictDetector.find(candidate: MeetingDraft, existing: List<MeetingDraft>): MeetingDraft?`

- [ ] **Step 1: Write failing week-mask tests**

```kotlin
@Test fun continuousWeeks_roundTripAndFormat() {
    val mask = WeekMask.fromWeeks((1..16).toSet(), 16)
    assertEquals((1..16).toSet(), WeekMask.toWeeks(mask, 16))
    assertEquals("1–16周", WeekMask.format(mask, 16))
}

@Test fun alternatingWeeks_roundTripAndFormat() {
    val weeks = setOf(1, 2, 4, 6, 8, 10)
    val mask = WeekMask.fromWeeks(weeks, 16)
    assertEquals(weeks, WeekMask.toWeeks(mask, 16))
    assertEquals("1–2、4、6、8、10周", WeekMask.format(mask, 16))
}
```

- [ ] **Step 2: Run `WeekMaskTest` and verify RED**

Expected: unresolved `WeekMask`.

- [ ] **Step 3: Implement `WeekMask` with bits 0–29 representing weeks 1–30**

Use bit shift and bitwise OR for encoding, require every week in `1..totalWeeks`, and compact only consecutive sequences of two or more weeks when formatting.

- [ ] **Step 4: Run `WeekMaskTest` and verify GREEN**

- [ ] **Step 5: Write failing validator and conflict tests**

Use this shared candidate shape:

```kotlin
data class MeetingDraft(
    val id: Long? = null,
    val courseId: Long? = null,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekMask: Long,
    val location: String = "",
)
```

Tests must prove:

```kotlin
@Test fun rejectsInvalidDayPeriodAndEmptyWeeks() {
    val errors = MeetingValidator.validate(
        MeetingDraft(weekday = 8, startPeriod = 5, endPeriod = 4, weekMask = 0L),
        totalWeeks = 16,
    )
    assertEquals(listOf("请选择星期", "结束节次不能早于开始节次", "请选择上课周次"), errors)
}

@Test fun overlappingPeriodAndWeek_conflicts() {
    val existing = MeetingDraft(id = 1, weekday = 3, startPeriod = 3, endPeriod = 4, weekMask = WeekMask.fromWeeks((1..8).toSet(), 16))
    val candidate = MeetingDraft(id = 2, weekday = 3, startPeriod = 4, endPeriod = 5, weekMask = WeekMask.fromWeeks(setOf(8, 9), 16))
    assertEquals(existing, ConflictDetector.find(candidate, listOf(existing)))
}

@Test fun adjacentPeriodsOrDisjointWeeks_doNotConflict() {
    val existing = MeetingDraft(id = 1, weekday = 3, startPeriod = 3, endPeriod = 4, weekMask = WeekMask.fromWeeks((1..8).toSet(), 16))
    val adjacent = existing.copy(id = 2, startPeriod = 5, endPeriod = 6)
    val laterWeeks = existing.copy(id = 3, weekMask = WeekMask.fromWeeks((9..16).toSet(), 16))
    assertNull(ConflictDetector.find(adjacent, listOf(existing)))
    assertNull(ConflictDetector.find(laterWeeks, listOf(existing)))
}

@Test fun editingSameMeeting_excludesItself() {
    val candidate = MeetingDraft(id = 7, weekday = 1, startPeriod = 3, endPeriod = 4, weekMask = 3L)
    assertNull(ConflictDetector.find(candidate, listOf(candidate)))
}
```

- [ ] **Step 6: Run validator and conflict tests and verify RED**

- [ ] **Step 7: Implement validation and conflict detection**

Period intervals overlap when `candidate.startPeriod <= other.endPeriod && other.startPeriod <= candidate.endPeriod`. Week sets overlap when `candidate.weekMask and other.weekMask != 0L`. Exclude only records whose non-null IDs match.

- [ ] **Step 8: Run all domain tests and verify GREEN**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.songkeyuan.schedule.domain.*'
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/domain app/src/test/java/com/songkeyuan/schedule/domain
git commit -m "feat: add schedule validation and conflict rules"
```

---

### Task 3: Room 模型、初始课表与数据库契约

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/CourseEntity.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/MeetingEntity.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/SemesterSettingsEntity.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/AppMetaEntity.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/CourseWithMeetings.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/LocalDateConverter.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/ScheduleDao.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/SeedCatalog.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/data/SeedCatalogTest.kt`
- Test: `app/src/androidTest/java/com/songkeyuan/schedule/data/local/ScheduleDaoTest.kt`

**Interfaces:**
- Produces: Room tables `courses`, `meetings`, `semester_settings`, `app_meta`.
- Produces: `SeedCatalog.courses: List<SeedCourse>` with exactly 15 courses and 14 meetings.
- Produces: `ScheduleDao.observeCourses(): Flow<List<CourseWithMeetings>>`
- Produces: `ScheduleDao.observeCourse(id: Long): Flow<CourseWithMeetings?>`
- Produces: insert, replace-meetings, delete, clear, settings, and meta DAO operations used by Task 4.

- [ ] **Step 1: Write a failing exact-data seed test**

```kotlin
@Test fun seedContainsTwelveScheduledAndThreePendingCourses() {
    assertEquals(15, SeedCatalog.courses.size)
    assertEquals(12, SeedCatalog.courses.count { it.meetings.isNotEmpty() })
    assertEquals(3, SeedCatalog.courses.count { it.meetings.isEmpty() })
    assertEquals(14, SeedCatalog.courses.sumOf { it.meetings.size })
}

@Test fun computerGraphics_usesCorrectRoomsByWeek() {
    val course = SeedCatalog.courses.single { it.code == "COST4168" }
    assertEquals(2, course.meetings.size)
    assertEquals(setOf(1, 2, 4, 6, 8, 10), WeekMask.toWeeks(course.meetings.single { it.location == "公共教学楼 A108" }.weekMask, 16))
    assertEquals(setOf(3, 5, 7, 9, 11), WeekMask.toWeeks(course.meetings.single { it.location == "12号楼 A443" }.weekMask, 16))
}

@Test fun pendingCourseCodesAndTeachers_arePreserved() {
    val pending = SeedCatalog.courses.filter { it.meetings.isEmpty() }.associate { it.code to it.teachers }
    assertEquals(mapOf("MARX1012-132" to "张晓敏", "COST4196-2" to "胡滨", "COST4121-2" to "任守纲"), pending)
}
```

- [ ] **Step 2: Run `SeedCatalogTest` and verify RED**

- [ ] **Step 3: Implement all seed records literally from specification section 6**

Use `WeekMask.fromWeeks` for every meeting. Keep room strings normalized with a single space between building name and room code. Do not copy class cohort text into the visible course name.

- [ ] **Step 4: Run `SeedCatalogTest` and verify GREEN**

- [ ] **Step 5: Write Room instrumentation tests**

Use `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)` and verify:

- inserting a course with one meeting is returned by `observeCourse`;
- a course with no meetings appears with an empty meeting list;
- deleting a course cascades to its meeting rows;
- updating `SemesterSettingsEntity` round-trips `LocalDate.of(2026, 8, 31)`.

- [ ] **Step 6: Run Room test and verify RED when a device is available**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.songkeyuan.schedule.data.local.ScheduleDaoTest
```

Expected before implementation: Room database/DAO symbols are unresolved. If no device is connected, retain the test and record that device execution is deferred to final verification.

- [ ] **Step 7: Implement entities, relations, converters, DAO, and database**

Use `@ForeignKey(onDelete = ForeignKey.CASCADE)` on `MeetingEntity.courseId`, a unique index on `app_meta.key`, and `@TypeConverters(LocalDateConverter::class)` on the database. Export Room schemas to `app/schemas` and commit them.

- [ ] **Step 8: Run JVM tests, build the database code, and run the Room test if available**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/data app/src/test/java/com/songkeyuan/schedule/data app/src/androidTest app/schemas
git commit -m "feat: add Room storage and initial timetable"
```

---

### Task 4: Repository、首次初始化与恢复课表

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/data/ScheduleModels.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/ScheduleStore.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/RoomScheduleStore.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/ScheduleRepository.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/data/RoomScheduleRepository.kt`
- Create: `app/src/test/java/com/songkeyuan/schedule/data/InMemoryScheduleStore.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/data/ScheduleRepositoryTest.kt`

**Interfaces:**
- Produces: `Course`, `Meeting`, `CourseDetails`, `ScheduledCourse`, `SemesterSettings`, `CourseDraft`.
- Produces: `ScheduleStore` with observable reads plus atomic `seedIfMissing`, `replaceCourse`, `deleteCourse`, `updateSettings`, and `resetToSeed` writes.
- Produces: `RoomScheduleStore` implementing `ScheduleStore` with `AppDatabase.withTransaction`.
- Produces: `sealed interface SaveOutcome { data class Success(val courseId: Long); data class Invalid(val messages: List<String>); data class Conflict(val courseName: String, val weekday: Int, val periods: IntRange, val weeks: Set<Int>); data class Failed(val message: String) }`
- Produces: `ScheduleRepository.observeDay(week: Int, weekday: Int): Flow<List<ScheduledCourse>>`
- Produces: `ScheduleRepository.observePending(): Flow<List<Course>>`
- Produces: `ScheduleRepository.observeCourse(id: Long): Flow<CourseDetails?>`
- Produces: `ScheduleRepository.observeSettings(): Flow<SemesterSettings>`
- Produces: `ensureSeeded()`, `saveCourse(draft)`, `deleteCourse(id)`, `updateSettings(settings)`, `resetToSeed()`.

- [ ] **Step 1: Write failing Repository behavior tests using the real in-memory store**

Tests must cover these observable results:

```kotlin
@Test fun ensureSeeded_runsOnceEvenAfterAllCoursesAreDeleted() = runTest {
    repository.ensureSeeded()
    assertEquals(15, store.courseCount())
    store.clearCoursesOnly()
    repository.ensureSeeded()
    assertEquals(0, store.courseCount())
}

@Test fun observeDay_filtersByWeekAndSortsByStartPeriod() = runTest {
    repository.ensureSeeded()
    val mondayWeekTwo = repository.observeDay(week = 2, weekday = 1).first()
    assertEquals(listOf("机器学习"), mondayWeekTwo.map { it.course.name })
    val mondayWeekTen = repository.observeDay(week = 10, weekday = 1).first()
    assertEquals(listOf("机器学习", "生涯规划与职业发展Ⅱ"), mondayWeekTen.map { it.course.name })
}

@Test fun pendingListContainsOnlyCoursesWithoutMeetings() = runTest {
    repository.ensureSeeded()
    assertEquals(listOf("形势与政策", "认知实习", "软件工程课程设计"), repository.observePending().first().map { it.name })
}

@Test fun conflictingSaveReturnsConflictWithoutWriting() = runTest {
    repository.ensureSeeded()
    val before = store.courseCount()
    val result = repository.saveCourse(conflictingFridayCourseDraft)
    assertTrue(result is SaveOutcome.Conflict)
    assertEquals(before, store.courseCount())
}

@Test fun resetDiscardsEditsAndRestoresDefaults() = runTest {
    repository.ensureSeeded()
    repository.updateSettings(SemesterSettings(LocalDate.of(2026, 9, 14), 20))
    repository.resetToSeed()
    assertEquals(15, store.courseCount())
    assertEquals(SemesterSettings(LocalDate.of(2026, 8, 31), 16), repository.observeSettings().first())
}
```

- [ ] **Step 2: Run `ScheduleRepositoryTest` and verify RED**

- [ ] **Step 3: Implement domain models and repository transaction boundaries**

`RoomScheduleStore.seedIfMissing` checks `app_meta["seed_version"]`; missing means insert settings, seed courses, seed meetings, then write value `1` in one transaction. `RoomScheduleRepository.saveCourse` trims text, validates every meeting, checks conflicts against all other meeting rows, and only then calls the store's atomic course-plus-meetings replacement. `RoomScheduleStore.resetToSeed` clears meetings and courses but preserves database structure, writes seed data and default settings, and keeps seed version `1`.

- [ ] **Step 4: Run Repository and all JVM tests and verify GREEN**

```bash
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/data app/src/test/java/com/songkeyuan/schedule/data
git commit -m "feat: add offline schedule repository"
```

---

### Task 5: 课程表 ViewModel 状态与交互

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/ScheduleUiState.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/ScheduleViewModel.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/ViewModelFactory.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/ui/schedule/ScheduleViewModelTest.kt`

**Interfaces:**
- Produces: `ScheduleUiState(selectedWeek, selectedWeekday, weekMonday, settings, courses, isLoading, errorMessage)`.
- Produces: `previousWeek()`, `nextWeek()`, `selectWeekday(weekday: Int)`, `retry()`.
- Consumes: `ScheduleRepository` and `today: () -> LocalDate`.

- [ ] **Step 1: Write failing ViewModel tests**

```kotlin
@Test fun startsOnCalculatedCurrentWeekAndDay() = runTest(testDispatcher) {
    val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 9, 8) })
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.selectedWeek)
    assertEquals(2, viewModel.uiState.value.selectedWeekday)
}

@Test fun weekNavigationStaysWithinSemester() = runTest(testDispatcher) {
    val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 8, 1) })
    viewModel.previousWeek()
    assertEquals(1, viewModel.uiState.value.selectedWeek)
    repeat(20) { viewModel.nextWeek() }
    assertEquals(16, viewModel.uiState.value.selectedWeek)
}

@Test fun selectingTuesdayLoadsTuesdayCourses() = runTest(testDispatcher) {
    val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 9, 7) })
    viewModel.selectWeekday(2)
    advanceUntilIdle()
    assertEquals(listOf("计算机操作系统", "Python程序设计语言", "Python程序设计语言实验"), viewModel.uiState.value.courses.map { it.course.name })
}
```

- [ ] **Step 2: Run `ScheduleViewModelTest` and verify RED**

- [ ] **Step 3: Implement a single StateFlow using `flatMapLatest` over selected week/day and settings**

Keep navigation bounds derived from current `SemesterSettings.totalWeeks`. Convert database errors into `errorMessage = "课程加载失败，请重试"` while keeping the last valid selection.

- [ ] **Step 4: Run ViewModel tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/ui/schedule app/src/main/java/com/songkeyuan/schedule/ui/ViewModelFactory.kt app/src/test/java/com/songkeyuan/schedule/ui
git commit -m "feat: add schedule screen state"
```

---

### Task 6: Material 3 应用外壳与每日时间轴首页

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/ScheduleApplication.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/MainActivity.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/AppContainer.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/App.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/AppDestination.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/theme/Color.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/theme/Type.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/ScheduleScreen.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/WeekHeader.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/DaySelector.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/schedule/CourseCard.kt`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Test: `app/src/androidTest/java/com/songkeyuan/schedule/ui/schedule/ScheduleScreenTest.kt`

**Interfaces:**
- Consumes: `ScheduleUiState` and callbacks for week/day selection, course click, add, and retry.
- Produces: destinations `schedule`, `pending`, `settings`, `editor/{courseId}` where `courseId = 0` means new course.

- [ ] **Step 1: Write failing Compose UI tests**

```kotlin
@Test fun scheduleShowsWeekDatesAndCourseDetails() {
    composeRule.setContent {
        ScheduleScreen(state = weekTwoMondayState, onPreviousWeek = {}, onNextWeek = {}, onSelectDay = {}, onCourseClick = {}, onAddCourse = {}, onRetry = {})
    }
    composeRule.onNodeWithText("第 2 周").assertIsDisplayed()
    composeRule.onNodeWithText("9月7日–9月13日").assertIsDisplayed()
    composeRule.onNodeWithText("机器学习").assertIsDisplayed()
    composeRule.onNodeWithText("第3–4节 · 公共教学楼 A106").assertIsDisplayed()
}

@Test fun emptyDayHasClearEmptyState() {
    composeRule.setContent {
        ScheduleScreen(state = sundayState, onPreviousWeek = {}, onNextWeek = {}, onSelectDay = {}, onCourseClick = {}, onAddCourse = {}, onRetry = {})
    }
    composeRule.onNodeWithText("今天没有已安排的课程").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the Compose test and verify RED on an available device**

- [ ] **Step 3: Implement application startup and the approved daily-timeline layout**

`ScheduleApplication.onCreate` launches `repository.ensureSeeded()` once in an application coroutine scope. `MainActivity` contains only edge-to-edge setup and `ScheduleApp()`. Use a `Scaffold` with Material 3 bottom navigation, a top week header, seven equal day buttons, `LazyColumn` course cards, and one “新建课程” floating action button. All icon-only controls receive Chinese `contentDescription` values.

- [ ] **Step 4: Run Compose tests when available and build Debug**

```bash
./gradlew :app:assembleDebug
```

Expected: launcher Activity resolves, app resources compile, and APK is generated.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/androidTest/java/com/songkeyuan/schedule/ui/schedule
git commit -m "feat: build daily schedule interface"
```

---

### Task 7: 课程编辑、保存、冲突提示与删除

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/editor/EditorUiState.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/editor/CourseEditorViewModel.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/editor/CourseEditorScreen.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/editor/MeetingEditor.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/editor/WeekPicker.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/ui/editor/CourseEditorViewModelTest.kt`
- Test: `app/src/androidTest/java/com/songkeyuan/schedule/ui/editor/CourseEditorScreenTest.kt`

**Interfaces:**
- Produces: editor events for field changes, add/remove meeting, save, request delete, confirm delete, dismiss message.
- Produces: one-shot navigation state only after successful save or delete.
- Consumes: `ScheduleRepository` and optional `courseId`.

- [ ] **Step 1: Write failing editor ViewModel tests**

Verify these behaviors with a real fake Repository implementation:

```kotlin
@Test fun blankNameDoesNotSave() = runTest(testDispatcher) {
    val viewModel = CourseEditorViewModel(repository, courseId = null)
    viewModel.setName("   ")
    viewModel.save()
    advanceUntilIdle()
    assertEquals("请输入课程名称", viewModel.uiState.value.errorMessage)
    assertEquals(0, repository.saveCalls)
}

@Test fun courseWithoutMeetingsSavesAsPending() = runTest(testDispatcher) {
    val viewModel = CourseEditorViewModel(repository, courseId = null)
    viewModel.setName("自定义课程")
    viewModel.save()
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.finished)
    assertEquals(emptyList<MeetingDraft>(), repository.lastDraft?.meetings)
}

@Test fun conflictMessageNamesCourseAndOverlap() = runTest(testDispatcher) {
    repository.nextSaveOutcome = SaveOutcome.Conflict("软件工程", 5, 1..2, setOf(2))
    val viewModel = CourseEditorViewModel(repository, courseId = null)
    viewModel.setName("冲突课程")
    viewModel.addMeeting(fridayPeriodsOneAndTwoWeekTwo)
    viewModel.save()
    advanceUntilIdle()
    assertEquals("与“软件工程”冲突：周五第1–2节，第2周", viewModel.uiState.value.errorMessage)
}

@Test fun deleteRequiresConfirmationAndNavigatesAfterSuccess() = runTest(testDispatcher) {
    val viewModel = CourseEditorViewModel(repository, courseId = 7L)
    viewModel.requestDelete()
    assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    viewModel.confirmDelete()
    advanceUntilIdle()
    assertEquals(listOf(7L), repository.deletedIds)
    assertTrue(viewModel.uiState.value.finished)
}
```

- [ ] **Step 2: Run editor ViewModel tests and verify RED**

- [ ] **Step 3: Implement editor state and ViewModel**

Keep all typed content after a failed save. Disable Save only while a save is active. Do not navigate on `Invalid`, `Conflict`, or `Failed`. Map repository errors to the exact Chinese messages asserted by tests.

- [ ] **Step 4: Run editor ViewModel tests and verify GREEN**

- [ ] **Step 5: Write and run Compose editor tests**

Tests cover visible labels, adding a second meeting, selecting weeks 1 and 3, field error display, save progress, and delete confirmation. Confirm that every tap target has a visible label or semantic description.

- [ ] **Step 6: Implement `CourseEditorScreen`, `MeetingEditor`, and `WeekPicker`**

Use Material 3 text fields and exposed dropdowns for weekday and periods. Render weeks 1–`totalWeeks` as selectable filter chips with “全选”和“清空” actions. Existing courses show “删除课程”; new courses do not.

- [ ] **Step 7: Run editor tests and full JVM suite**

```bash
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/ui/editor app/src/test/java/com/songkeyuan/schedule/ui/editor app/src/androidTest/java/com/songkeyuan/schedule/ui/editor
git commit -m "feat: add editable course workflow"
```

---

### Task 8: 待安排列表与学期设置

**Files:**
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/pending/PendingUiState.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/pending/PendingViewModel.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/pending/PendingScreen.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/settings/SettingsUiState.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/songkeyuan/schedule/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/ui/pending/PendingViewModelTest.kt`
- Test: `app/src/test/java/com/songkeyuan/schedule/ui/settings/SettingsViewModelTest.kt`
- Test: `app/src/androidTest/java/com/songkeyuan/schedule/ui/PendingAndSettingsScreenTest.kt`

**Interfaces:**
- Pending screen consumes `observePending()` and navigates to `editor/{id}`.
- Settings screen edits `firstWeekMonday`, edits `totalWeeks`, and exposes a confirmed reset action.

- [ ] **Step 1: Write failing pending and settings ViewModel tests**

Assert the initial pending names exactly, verify a course disappears when a meeting is added, reject total weeks 0 and 31 with `总周数请输入 1–30`, persist a valid start date and week count, and require confirmation before `resetToSeed()`.

- [ ] **Step 2: Run tests and verify RED**

- [ ] **Step 3: Implement pending and settings ViewModels**

Settings save returns to a non-editing state only after Repository success. Reset restores `2026-08-31`, 16 weeks, 15 courses, and 14 meetings.

- [ ] **Step 4: Run ViewModel tests and verify GREEN**

- [ ] **Step 5: Write failing Compose tests for pending and settings screens**

The pending test asserts “待安排 · 3” and the three names. The settings test changes the date to `2026-09-07`, changes weeks to `18`, taps Save, and verifies no validation message. The reset test verifies the confirmation dialog text `恢复后将清除所有自定义修改`.

- [ ] **Step 6: Implement both screens and wire bottom navigation**

Use a simple list for pending courses and native Material date picker/text input for the semester start. Preserve system back behavior and selected bottom-navigation destination.

- [ ] **Step 7: Run all available automated tests**

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

If no device is available, run the JVM target and retain the connected-test command for Task 9.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/songkeyuan/schedule/ui/pending app/src/main/java/com/songkeyuan/schedule/ui/settings app/src/test app/src/androidTest
git commit -m "feat: add pending courses and semester settings"
```

---

### Task 9: README、全量验证与 APK 交付

**Files:**
- Create: `README.md`
- Modify: `.gitignore`
- Output: `app/build/outputs/apk/debug/app-debug.apk`
- Copy output: `outputs/我的课表-2026-2027-1-debug.apk`

**Interfaces:**
- Produces: installable signed Debug APK and reproducible build instructions.

- [ ] **Step 1: Write README**

Document:

- supported Android version: 8.0 and above;
- installation from the APK file and enabling “允许来自此来源的应用”;
- fallback ADB command `adb install -r outputs/我的课表-2026-2027-1-debug.apk`;
- build command `./gradlew clean testDebugUnitTest lintDebug assembleDebug`;
- all data stays on device and uninstalling clears it;
- initial date, 16-week setting, 12 scheduled courses, and 3 pending courses;
- restoring the original timetable deletes custom edits;
- debug-signed APK is for personal installation, not Play Store release.

- [ ] **Step 2: Run the complete verification gate**

```bash
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Expected: exit code 0, zero failed unit tests, zero Lint errors, and a fresh `app-debug.apk`.

- [ ] **Step 3: Run device tests when an emulator or phone is available**

```bash
adb devices
./gradlew :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.songkeyuan.schedule/.MainActivity
```

Verify on screen: week 2 for 2026-09-07, Monday shows machine learning, Tuesday shows three courses, Thursday week 3 shows computer graphics in `12号楼 A443`, adding a pending course meeting moves it to the schedule, and reset restores the original data.

- [ ] **Step 4: Inspect APK metadata and permissions**

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk
$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

Expected: package `com.songkeyuan.schedule`, `sdkVersion:'26'`, valid Debug signature, and no `android.permission.INTERNET` line.

- [ ] **Step 5: Copy the freshly verified APK to the delivery path and checksum it**

```bash
mkdir -p outputs
cp app/build/outputs/apk/debug/app-debug.apk outputs/我的课表-2026-2027-1-debug.apk
shasum -a 256 outputs/我的课表-2026-2027-1-debug.apk
```

- [ ] **Step 6: Review requirements line by line**

Compare the implementation with every “包含”“不包含”“校验与错误处理”“APK 验收” item in the specification. Record any device-test limitation factually; do not claim device installation if no emulator or phone was available.

- [ ] **Step 7: Commit the documentation and final verified state**

```bash
git add README.md .gitignore app outputs/我的课表-2026-2027-1-debug.apk
git commit -m "docs: add APK installation and verification guide"
```
