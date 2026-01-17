package com.example.smartlock.data.model
import kotlinx.serialization.Serializable

@Serializable
data class DoorLog(
    val id: String,
    val rfid_uid: String?,
    val action: String,
    val method: String,
    val created_at: String
)
