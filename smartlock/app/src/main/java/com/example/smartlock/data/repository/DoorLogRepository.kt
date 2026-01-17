package com.example.smartlock.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.smartlock.data.model.DoorLog
import com.example.smartlock.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID

object DoorLogRepository {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insertLogFromEvent(
        event: String,       // "UNLOCK" hoặc "DENIED"
        method: String,
        rfidUid: String?
    ) {
        // Map event MQTT sang action
        val action = when(event) {
            "UNLOCK" -> "OPEN"
            "DENIED" -> "DENIED"
            else -> "UNKNOWN"
        }

        val log = DoorLog(
            id = UUID.randomUUID().toString(),
            rfid_uid = rfidUid,
            action = action,
            method = method,
            created_at = Instant.now().toString()
        )

        SupabaseClient.client
            .from("door_logs")
            .insert(log)
    }
}
