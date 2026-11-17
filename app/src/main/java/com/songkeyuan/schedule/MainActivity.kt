package com.songkeyuan.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.songkeyuan.schedule.ui.ScheduleApp
import com.songkeyuan.schedule.ui.theme.ClassScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassScheduleTheme {
                ScheduleApp((application as ScheduleApplication).container.repository)
            }
        }
    }
}
