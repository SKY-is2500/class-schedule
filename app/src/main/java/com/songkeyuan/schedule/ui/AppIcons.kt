package com.songkeyuan.schedule.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.songkeyuan.schedule.R

enum class AppIconType(val resource: Int) {
    Calendar(R.drawable.ic_calendar), Pending(R.drawable.ic_pending), Settings(R.drawable.ic_settings),
    Add(R.drawable.ic_add), Previous(R.drawable.ic_previous), Next(R.drawable.ic_next),
}

@Composable fun AppIcon(type: AppIconType, modifier: Modifier = Modifier, description: String? = null) {
    Icon(painterResource(type.resource), contentDescription=description, modifier=modifier)
}
