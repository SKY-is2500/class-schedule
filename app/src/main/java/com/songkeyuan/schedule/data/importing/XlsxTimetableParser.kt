package com.songkeyuan.schedule.data.importing

import com.songkeyuan.schedule.data.CourseDraft
import com.songkeyuan.schedule.domain.MeetingDraft
import com.songkeyuan.schedule.domain.MeetingValidator
import com.songkeyuan.schedule.domain.WeekMask
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Reads the school's exported weekly grid. No Office library or network is required. */
class XlsxTimetableParser {
    fun parse(input: InputStream): ImportedTimetable {
        try {
            return readWorkbook(input)
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("无法读取 Excel，请选择教务系统导出的 .xlsx 课表", error)
        }
    }

    private fun readWorkbook(input: InputStream): ImportedTimetable {
        val compressed = input.readBytesLimited(MAX_COMPRESSED)
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0
        ZipInputStream(compressed.inputStream()).use { zip ->
            var entry = zip.nextEntry
            var count = 0
            while (entry != null) {
                require(++count <= 1000) { "Excel 文件内容过多" }
                // Bound every entry, including ones we do not consume (ZIP bomb protection).
                val bytes = zip.readBytesLimited(MAX_XML)
                total += bytes.size
                require(total <= MAX_EXPANDED) { "Excel 文件过大，请选择单份学期课表" }
                if (entry.name == "xl/sharedStrings.xml" || entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml"))) {
                    entries[entry.name] = bytes
                }
                entry = zip.nextEntry
            }
        }
        val strings = entries["xl/sharedStrings.xml"]?.let { bytes ->
            xml(bytes).getElementsByTagName("si").let { nodes ->
                (0 until nodes.length).map { (nodes.item(it) as Element).textNodes("t") }
            }
        } ?: emptyList()
        val sheets = entries.filterKeys { it.startsWith("xl/worksheets/") }.values.map { bytes ->
            val cells = linkedMapOf<String, String>()
            val nodes = xml(bytes).getElementsByTagName("c")
            for (i in 0 until nodes.length) {
                val cell = nodes.item(i) as Element
                val value = when (cell.getAttribute("t")) {
                    "s" -> strings.getOrNull(cell.textNodes("v").toIntOrNull() ?: -1)
                        ?: throw IllegalArgumentException("Excel 字符串索引无效")
                    "inlineStr" -> cell.textNodes("t")
                    else -> cell.textNodes("v")
                }
                if (value.isNotBlank()) cells[cell.getAttribute("r")] = value.trim()
            }
            cells
        }
        val cells = sheets.firstOrNull { sheet ->
            (1..7).all { day -> sheet.values.any { it == "星期${DAY_NAMES[day - 1]}" } }
        } ?: throw IllegalArgumentException("未识别到周一至周日课表，请使用与示例相同格式的教务系统 .xlsx 文件")
        val dayColumns = (1..7).associate { day ->
            val address = cells.entries.first { it.value == "星期${DAY_NAMES[day - 1]}" }.key
            address.takeWhile { it.isLetter() } to day
        }
        val courses = linkedMapOf<String, CourseDraft>()
        val periodRows = cells.filterValues { it.matches(Regex("第[一二三四五六七八九十]+节")) }
            .keys.map { it.dropWhile(Char::isLetter).toInt() }.toSet()
        cells.forEach { (address, text) ->
            val col = address.takeWhile { it.isLetter() }
            val day = dayColumns[col] ?: return@forEach
            val row = address.drop(col.length).toInt()
            if (row !in periodRows || text in setOf("无课", "自修", "自习")) return@forEach
            val courseMatch = COURSE.matchEntire(text)
                ?: throw IllegalArgumentException("${address} 的课程内容无法完整识别：${text.take(60)}")
            val name = courseMatch.groupValues[1].trim()
            val code = courseMatch.groupValues[2]
            val teachers = cleanTeachers(cells["$col${row + 1}"].orEmpty())
            val location = cells["$col${row + 3}"].orEmpty()
            val schedule = cells["$col${row + 4}"]
                ?: throw IllegalArgumentException("$name 缺少周次或节次，无法完整导入")
            val meeting = parseMeeting(schedule, location)
            require(meeting.weekday == day) { "$name 的星期与节次编码不一致" }
            addCourse(courses, name, code, teachers, meeting)
        }
        val remarkEntries = cells.entries.filter { it.value == "备注" }.flatMap { (address, _) ->
            val row = address.dropWhile { it.isLetter() }
            cells.filterKeys { it.dropWhile(Char::isLetter) == row && it != address }.values
        }
        remarkEntries.forEach { original ->
            var remaining = original
            REMARK_MEETING.findAll(original).forEach { match ->
                addCourse(courses, match.groupValues[1].trim(), match.groupValues[2],
                    cleanTeachers(match.groupValues[3]),
                    parseMeeting(match.groupValues[4], match.groupValues[5].trim().trimEnd(',', '，')))
                remaining = remaining.replace(match.value, "")
            }
            remaining.split(';', '；').filter { it.isNotBlank() }.forEach { part ->
                val pending = PENDING.find(part)
                if (pending != null) {
                    addCourse(courses, pending.groupValues[3].trim(),
                        pending.groupValues[1] + pending.groupValues[2], cleanTeachers(pending.groupValues[4]), null)
                } else {
                    require(part.trim().trim(',', '，').isBlank()) {
                        "备注内容无法完整识别：${part.trim().take(60)}"
                    }
                }
            }
        }
        require(courses.isNotEmpty()) { "Excel 中没有识别到课程" }
        val drafts = courses.values.toList()
        require(drafts.size <= 300 && drafts.sumOf { it.meetings.size } <= 1000) { "课表课程过多" }
        val requiredWeeks = drafts.flatMap { it.meetings }.flatMap { WeekMask.toWeeks(it.weekMask, 30) }.maxOrNull() ?: 1
        return ImportedTimetable(drafts, requiredWeeks)
    }

    private fun addCourse(courses: MutableMap<String, CourseDraft>, name: String, code: String, teachers: String, meeting: MeetingDraft?) {
        val previous = courses[code]
        require(previous == null || previous.name == name) { "课程代码 $code 对应多个课程名称" }
        val allTeachers = (previous?.teachers.orEmpty().split('、') + teachers.split('、')).filter { it.isNotBlank() }.distinct().joinToString("、")
        courses[code] = (previous ?: CourseDraft(name = name, code = code)).copy(
            teachers = allTeachers,
            meetings = (previous?.meetings.orEmpty() + listOfNotNull(meeting)).distinct(),
        )
    }

    private fun parseMeeting(value: String, location: String): MeetingDraft {
        val match = SCHEDULE.matchEntire(value.trim()) ?: throw IllegalArgumentException("无法完整识别上课安排：$value")
        val weeks = linkedSetOf<Int>()
        match.groupValues[1].replace('，', ',').replace('、', ',').split(',').forEach { token ->
            val range = token.trim().split(Regex("[-–—~～]"))
            val start = range.first().trim().toIntOrNull() ?: throw IllegalArgumentException("上课周次无效：$value")
            val end = range.last().trim().toIntOrNull() ?: start
            require(start in 1..30 && end in start..30) { "上课周次应在 1–30 之间" }
            weeks += start..end
        }
        val encoded = match.groupValues[2]
        require(encoded.length >= 3 && encoded.length % 2 == 1) { "无法识别节次：$value" }
        val weekday = encoded.first().digitToInt()
        val periods = encoded.drop(1).chunked(2).map { it.toInt() }
        require(periods == (periods.first()..periods.last()).toList()) { "不连续的节次暂不支持：$value" }
        val draft = MeetingDraft(weekday = weekday, startPeriod = periods.first(), endPeriod = periods.last(),
            weekMask = WeekMask.fromWeeks(weeks,30), location = location.trim())
        require(MeetingValidator.validate(draft,30).isEmpty()) { "上课安排超出有效范围：$value" }
        return draft
    }

    private fun cleanTeachers(raw: String): String = raw.replace(Regex("\\[[^]]*]")) { match ->
        val content = match.value.drop(1).dropLast(1)
        if (content.matches(Regex("[A-Za-z0-9]+"))) "" else content
    }.split(',', '，', ';', '；', '、').map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString("、")

    private fun xml(bytes: ByteArray): Element {
        val text = bytes.toString(Charsets.UTF_8)
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(text)) { "不支持包含外部实体的 Excel" }
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bytes.inputStream()).documentElement
    }

    private fun Element.textNodes(tag: String): String = getElementsByTagName(tag).let { nodes ->
        (0 until nodes.length).joinToString("") { nodes.item(it).textContent }
    }

    private fun InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = read(buffer)
        while (count != -1) {
            require(output.size() + count <= limit) { "Excel 文件过大，请选择单份学期课表" }
            output.write(buffer,0,count)
            count = read(buffer)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_COMPRESSED = 10 * 1024 * 1024
        const val MAX_XML = 5 * 1024 * 1024
        const val MAX_EXPANDED = 20 * 1024 * 1024
        val DAY_NAMES = listOf("一","二","三","四","五","六","日")
        val COURSE = Regex("^(.+?)\\s+([A-Z][A-Z0-9]+(?:-\\d+)?)$")
        val SCHEDULE = Regex("([0-9,，、\\s–—~～-]+)周\\[(\\d+)节]")
        val REMARK_MEETING = Regex("([^,，;；]+?)\\s+([A-Z][A-Z0-9]+)\\s+\\[([^]]*)]\\s+\\[[^]]*(?:][^]]*)*?]\\s+\\[星期[一二三四五六日]]\\s+([0-9,，、\\s–—~～-]+周\\[\\d+节])\\s+([^;；]+?)(?=,\\s*\\[|，\\s*\\[|$)")
        val PENDING = Regex("\\[([A-Z][A-Z0-9]+)](-\\d+)?\\s+([^\\[]+?)\\s+((?:\\[[^]]*][^;；]*)+)")
    }
}
