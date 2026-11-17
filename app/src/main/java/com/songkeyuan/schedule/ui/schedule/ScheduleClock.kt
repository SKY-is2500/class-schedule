package com.songkeyuan.schedule.ui.schedule

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Refresh on resume and while visible, including across midnight. */
@Composable fun rememberScheduleClock(): LocalDateTime {
    val owner = LocalLifecycleOwner.current
    val now by produceState(LocalDateTime.now(), owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = LocalDateTime.now()
                delay(30_000)
            }
        }
    }
    return now
}
