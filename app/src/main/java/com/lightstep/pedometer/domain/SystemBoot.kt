package com.lightstep.pedometer.domain

import android.os.SystemClock
import kotlin.math.roundToLong

object SystemBoot {
    fun currentBootId(): String {
        val bootStartedAtMinutes = ((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60000.0).roundToLong()
        return bootStartedAtMinutes.toString()
    }
}
