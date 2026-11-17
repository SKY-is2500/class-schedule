package com.songkeyuan.schedule.ui

enum class AppDestination(val route: String, val label: String, val symbol: String) {
    Schedule("schedule", "课表", "▦"),
    Pending("pending", "待安排", "◷"),
    Settings("settings", "设置", "⚙"),
}
