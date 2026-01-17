package com.example.smartlock.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {

    private val VN_TZ = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    private val isoFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val pgFormat = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSSSSSX",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val outputFormat = SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale("vi", "VN")
    ).apply {
        timeZone = VN_TZ
    }

    fun formatToVN(time: String?): String {
        if (time.isNullOrBlank()) return "Không rõ thời gian"

        val date = try {
            when {
                time.contains("T") -> isoFormat.parse(time)
                else -> pgFormat.parse(time)
            }
        } catch (e: Exception) {
            null
        }

        return date?.let { outputFormat.format(it) }
            ?: "Không rõ thời gian"
    }
}
