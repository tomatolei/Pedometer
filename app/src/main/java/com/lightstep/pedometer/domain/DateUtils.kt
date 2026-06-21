package com.lightstep.pedometer.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun dateFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun todayString(): String =
    dateFormatter().format(Date())

fun dateString(timestampMillis: Long): String =
    dateFormatter().format(Date(timestampMillis))

fun daysAgoString(daysAgo: Long): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo.toInt())
    return dateFormatter().format(calendar.time)
}
