package com.songkeyuan.schedule.ui

enum class AppDestination(val route:String,val label:String,val icon:AppIconType) {
    Schedule("schedule","课表",AppIconType.Calendar),
    Pending("pending","待安排",AppIconType.Pending),
    Settings("settings","设置",AppIconType.Settings),
}
