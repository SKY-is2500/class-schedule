package com.songkeyuan.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.songkeyuan.schedule.data.Course
import com.songkeyuan.schedule.ui.schedule.courseColorIndex

data class CourseColors(val container: Color, val ink: Color, val accent: Color)

private val lightCourses = listOf(
    CourseColors(Color(0xFFE3EDFF), Color(0xFF193E74), Color(0xFF416FB2)),
    CourseColors(Color(0xFFDBF1E8), Color(0xFF174E3D), Color(0xFF328267)),
    CourseColors(Color(0xFFF0E6FA), Color(0xFF593773), Color(0xFF8A5CA8)),
    CourseColors(Color(0xFFFFE9DB), Color(0xFF793E20), Color(0xFFB76A3B)),
    CourseColors(Color(0xFFF9E2EA), Color(0xFF73334C), Color(0xFFAC5A79)),
    CourseColors(Color(0xFFDCF0F3), Color(0xFF205361), Color(0xFF398291)),
    CourseColors(Color(0xFFF4EFCF), Color(0xFF625319), Color(0xFF938027)),
    CourseColors(Color(0xFFE7E8F7), Color(0xFF3F4277), Color(0xFF6B6EA5)),
)
private val darkCourses = listOf(
    CourseColors(Color(0xFF203854), Color(0xFFD6E6FF), Color(0xFF9BBFF2)),
    CourseColors(Color(0xFF1E4035), Color(0xFFD0F0DF), Color(0xFF8ACFAE)),
    CourseColors(Color(0xFF3C2C4E), Color(0xFFEEDCFF), Color(0xFFCAACE8)),
    CourseColors(Color(0xFF4C3225), Color(0xFFFFE1CA), Color(0xFFE5AE82)),
    CourseColors(Color(0xFF4B2B3B), Color(0xFFFFDAE7), Color(0xFFE8A3BE)),
    CourseColors(Color(0xFF1E3D46), Color(0xFFCFEFF6), Color(0xFF8FCAD9)),
    CourseColors(Color(0xFF403B20), Color(0xFFF6ECC1), Color(0xFFD3C57F)),
    CourseColors(Color(0xFF30334E), Color(0xFFE0E2FF), Color(0xFFB5B9E8)),
)

@Composable fun courseColors(course: Course): CourseColors =
    (if (MaterialTheme.colorScheme.background.luminance() < .5f) darkCourses else lightCourses)[courseColorIndex(course)]
