package com.example.smartlock.data.model
import kotlinx.serialization.Serializable

@Serializable
data class RfidUser(
    val id: String,
    val uid: String,
    val name: String?,
    val enabled: Boolean = true,
    val created_at: String? = null
)
