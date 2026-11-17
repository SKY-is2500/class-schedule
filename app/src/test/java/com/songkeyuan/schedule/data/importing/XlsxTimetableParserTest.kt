package com.songkeyuan.schedule.data.importing

import com.songkeyuan.schedule.domain.WeekMask
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

class XlsxTimetableParserTest {
    private fun sample() = javaClass.getResourceAsStream("/sample-timetable.xlsx")!!

    @Test fun readsRealExportWithoutDuplicatingEveryPeriod() {
        val result = sample().use { XlsxTimetableParser().parse(it) }
        assertEquals(15, result.courses.size)
        assertEquals(15, result.courses.sumOf { it.meetings.size })
        assertEquals(3, result.courses.count { it.meetings.isEmpty() })
        assertEquals(16, result.requiredWeeks)
    }

    @Test fun preservesRescheduledSaturdayAndTeachersAndRemarkRoom() {
        val courses = sample().use { XlsxTimetableParser().parse(it) }.courses
        val network = courses.single { it.code == "COST3138" }
        assertEquals(setOf(1,2,3,4,5,7,8), WeekMask.toWeeks(network.meetings.single { it.weekday == 3 }.weekMask,30))
        assertEquals(setOf(6), WeekMask.toWeeks(network.meetings.single { it.weekday == 6 }.weekMask,30))
        assertEquals(3, network.meetings.single { it.weekday == 6 }.startPeriod)
        assertEquals(4, network.meetings.single { it.weekday == 6 }.endPeriod)
        val graphics = courses.single { it.code == "COST4168" }
        assertEquals(setOf(3,5,7,9,11),WeekMask.toWeeks(graphics.meetings.single { it.location == "12号楼A443" }.weekMask,30))
        assertEquals("张晓敏、施晓琳", courses.single { it.code == "MARX1012-132" }.teachers)
    }

    @Test fun rejectsGarbageInsteadOfShowingAnEmptySuccessfulImport() {
        assertThrows(IllegalArgumentException::class.java) { XlsxTimetableParser().parse(ByteArrayInputStream("not excel".toByteArray())) }
    }

    @Test fun rejectsFileAboveCompressedSizeLimit() {
        assertThrows(IllegalArgumentException::class.java) { XlsxTimetableParser().parse(ByteArrayInputStream(ByteArray(10*1024*1024+1))) }
    }

    @Test fun rejectsUnrelatedWorkbook() {
        val data = ByteArrayOutputStream()
        ZipOutputStream(data).use { zip ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>其他文件</t></is></c></row></sheetData></worksheet>""".toByteArray())
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { XlsxTimetableParser().parse(data.toByteArray().inputStream()) }
    }

    private fun modifiedSample(from:String,to:String):ByteArray {
        val data=ByteArrayOutputStream()
        ZipOutputStream(data).use { output ->
            sample().use { source -> ZipInputStream(source).use { input ->
                var entry=input.nextEntry
                while(entry!=null) {
                    val bytes=input.readBytes()
                    output.putNextEntry(ZipEntry(entry.name))
                    output.write(if(entry.name=="xl/sharedStrings.xml") bytes.toString(Charsets.UTF_8).replace(from,to).toByteArray() else bytes)
                    output.closeEntry()
                    entry=input.nextEntry
                }
            } }
        }
        return data.toByteArray()
    }
    @Test fun rejectsMixedRecognizedAndUnrecognizedCourseBlocks() {
        val bytes=modifiedSample("软件工程 COST4106","软件工程（格式变化）")
        assertThrows(IllegalArgumentException::class.java) { XlsxTimetableParser().parse(bytes.inputStream()) }
    }
    @Test fun rejectsUnconsumedExtraScheduleInsteadOfDroppingIt() {
        val bytes=modifiedSample("1-16周[50102节]","1-16周[50102节] 1周[60102节]")
        assertThrows(IllegalArgumentException::class.java) { XlsxTimetableParser().parse(bytes.inputStream()) }
    }
}
