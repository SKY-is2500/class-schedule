package com.songkeyuan.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.songkeyuan.schedule.ui.ScheduleApp
import com.songkeyuan.schedule.ui.theme.ClassScheduleTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val widgetRequests = Channel<Unit>(Channel.CONFLATED)
    private val widgetOpenRequests = widgetRequests.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleWidgetIntent(intent)
        enableEdgeToEdge()
        setContent {
            ClassScheduleTheme {
                ScheduleApp((application as ScheduleApplication).container.repository, widgetOpenRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent) {
        if (intent.action == ACTION_OPEN_TODAY) widgetRequests.trySend(Unit)
    }

    companion object {
        internal const val ACTION_OPEN_TODAY = "com.songkeyuan.schedule.action.OPEN_TODAY"
    }
}
