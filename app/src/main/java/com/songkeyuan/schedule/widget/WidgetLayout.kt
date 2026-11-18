package com.songkeyuan.schedule.widget

internal data class WidgetLayout(
    val visibleCourses: Int,
    val showTeachingWeek: Boolean,
    val showOverflowCount: Boolean,
) {
    companion object {
        fun forSize(widthDp: Float, heightDp: Float, fontScale: Float, courseCount: Int): WidgetLayout {
            // Allow for the widget padding, heading, and each course's two lines of text.
            val contentHeight = heightDp - (25f + 24f * fontScale)
            val rowHeight = 4f + 38f * fontScale
            val footerHeight = 4f + 18f * fontScale
            val rowsWithoutFooter = (contentHeight / rowHeight).toInt().coerceIn(1, 6)
            val showFooter = courseCount > rowsWithoutFooter && contentHeight >= rowHeight + footerHeight
            val rowCapacity = if (showFooter) {
                ((contentHeight - footerHeight) / rowHeight).toInt().coerceIn(1, 6)
            } else {
                rowsWithoutFooter
            }
            return WidgetLayout(
                visibleCourses = minOf(courseCount, rowCapacity),
                showTeachingWeek = widthDp >= 210f * fontScale.coerceAtLeast(1f),
                showOverflowCount = showFooter,
            )
        }
    }
}
