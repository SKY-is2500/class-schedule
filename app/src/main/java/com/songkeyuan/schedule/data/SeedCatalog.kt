package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.WeekMask

data class SeedMeeting(
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekMask: Long,
    val location: String,
)

data class SeedCourse(
    val name: String,
    val code: String,
    val teachers: String,
    val meetings: List<SeedMeeting> = emptyList(),
)

object SeedCatalog {
    private const val TOTAL_WEEKS = 16
    private val allWeeks = WeekMask.fromWeeks((1..16).toSet(), TOTAL_WEEKS)

    private fun meeting(
        weekday: Int,
        startPeriod: Int,
        endPeriod: Int,
        weeks: Set<Int> = (1..16).toSet(),
        location: String,
    ) = SeedMeeting(
        weekday = weekday,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        weekMask = if (weeks.size == TOTAL_WEEKS) allWeeks else WeekMask.fromWeeks(weeks, TOTAL_WEEKS),
        location = location,
    )

    val courses: List<SeedCourse> = listOf(
        SeedCourse(
            name = "软件工程",
            code = "COST4106",
            teachers = "任守纲、薛卫",
            meetings = listOf(meeting(5, 1, 2, location = "公共教学楼 A104")),
        ),
        SeedCourse(
            name = "机器学习",
            code = "COST4160",
            teachers = "舒欣",
            meetings = listOf(meeting(1, 3, 4, location = "公共教学楼 A106")),
        ),
        SeedCourse(
            name = "计算机操作系统",
            code = "COST3128",
            teachers = "姜海燕、徐灿",
            meetings = listOf(meeting(2, 3, 5, location = "公共教学楼 A108")),
        ),
        SeedCourse(
            name = "计算机网络安全",
            code = "COST3138",
            teachers = "赵青松",
            meetings = listOf(
                meeting(3, 3, 4, (1..8).toSet(), "公共教学楼 A110"),
                meeting(5, 3, 4, (1..8).toSet(), "公共教学楼 C204"),
            ),
        ),
        SeedCourse(
            name = "计算机图形学",
            code = "COST4168",
            teachers = "谢忠红",
            meetings = listOf(
                meeting(4, 3, 5, setOf(1, 2, 4, 6, 8, 10), "公共教学楼 A108"),
                meeting(4, 3, 5, setOf(3, 5, 7, 9, 11), "12号楼 A443"),
            ),
        ),
        SeedCourse(
            name = "Python程序设计语言",
            code = "COST4175",
            teachers = "谢元澄、沈毅",
            meetings = listOf(meeting(2, 6, 7, location = "公共教学楼 A110")),
        ),
        SeedCourse(
            name = "计算机视觉",
            code = "COST4199",
            teachers = "顾兴健",
            meetings = listOf(meeting(4, 6, 7, location = "公共教学楼 A104")),
        ),
        SeedCourse(
            name = "生涯规划与职业发展Ⅱ",
            code = "GC1102",
            teachers = "李洪",
            meetings = listOf(meeting(1, 8, 9, (9..12).toSet(), "公共教学楼 D107")),
        ),
        SeedCourse(
            name = "Python程序设计语言实验",
            code = "COST3143",
            teachers = "谢元澄、沈毅",
            meetings = listOf(meeting(2, 8, 9, location = "12号楼 A449")),
        ),
        SeedCourse(
            name = "神经网络与深度学习",
            code = "COST4188",
            teachers = "翟肇裕",
            meetings = listOf(meeting(4, 8, 9, location = "公共教学楼 A104")),
        ),
        SeedCourse(
            name = "最优化理论与方法",
            code = "COST4186",
            teachers = "罗艳",
            meetings = listOf(meeting(5, 8, 9, location = "公共教学楼 A104")),
        ),
        SeedCourse(
            name = "海岸带资源学研究进展",
            code = "ARGE4001W",
            teachers = "周兆胜、何梅琳",
            meetings = listOf(meeting(3, 10, 11, (9..16).toSet(), "公共教学楼 B131")),
        ),
        SeedCourse(name = "形势与政策", code = "MARX1012-132", teachers = "张晓敏"),
        SeedCourse(name = "认知实习", code = "COST4196-2", teachers = "胡滨"),
        SeedCourse(name = "软件工程课程设计", code = "COST4121-2", teachers = "任守纲"),
    )
}
