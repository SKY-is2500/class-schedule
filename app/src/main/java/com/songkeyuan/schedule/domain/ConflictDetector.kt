package com.songkeyuan.schedule.domain

object ConflictDetector {
    fun find(candidate: MeetingDraft, existing: List<MeetingDraft>): MeetingDraft? =
        existing.firstOrNull { other ->
            !isSameRecord(candidate, other) &&
                candidate.weekday == other.weekday &&
                candidate.startPeriod <= other.endPeriod &&
                other.startPeriod <= candidate.endPeriod &&
                candidate.weekMask and other.weekMask != 0L
        }

    private fun isSameRecord(candidate: MeetingDraft, other: MeetingDraft): Boolean =
        candidate.id != null && candidate.id == other.id
}
