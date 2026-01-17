package com.example.smartlock.mqtt

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.smartlock.data.model.DoorLog
import com.example.smartlock.data.supabase.SupabaseClient
import com.example.smartlock.util.NotificationHelper
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.paho.client.mqttv3.*
import java.time.Instant
import java.util.*

object MqttManager {

    private const val TAG = "MQTT"
    private const val SERVER_URI =
        "ssl://1c96de2709a740a68e5b6d81365811b2.s1.eu.hivemq.cloud:8883"
    private const val USER = "duyduong"
    private const val PASS = "Duy10012004/"

    private lateinit var client: MqttClient
    private var appContext: Context? = null

    /* ================= DEVICE ONLINE STATE ================= */

    private val _deviceOnlineFlow = MutableStateFlow(false)
    val deviceOnlineFlow = _deviceOnlineFlow

    private var lastMessageTime = 0L
    private const val OFFLINE_TIMEOUT = 10_000L // 10 giây

    /* ================= RFID FLOW ================= */

    private val _rfidScanFlow = MutableSharedFlow<String>()
    val rfidScanFlow = _rfidScanFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /* ================= CONNECT ================= */

    fun connect(context: Context) {
        appContext = context.applicationContext

        try {
            client = MqttClient(SERVER_URI, MqttClient.generateClientId(), null)

            val options = MqttConnectOptions().apply {
                userName = USER
                password = PASS.toCharArray()
                isAutomaticReconnect = true
                isCleanSession = true
            }

            client.setCallback(object : MqttCallback {

                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "❌ MQTT connection lost", cause)
                }


                @RequiresApi(Build.VERSION_CODES.O)
                override fun messageArrived(topic: String?, message: MqttMessage?) {

                    lastMessageTime = System.currentTimeMillis()
                    _deviceOnlineFlow.value = true

                    // 🚫 BỎ QUA MESSAGE RETAIN
                    if (message?.isRetained == true) {
                        Log.w(TAG, "⚠️ Ignore retained log: ${message.toString()}")
                        return
                    }

                    val payload = message.toString()

                    if (topic == "lock/status") return

                    if (topic == "lock/log") {

                        val friendlyMessage = getEasyMessage(payload)
                        appContext?.let {
                            NotificationHelper.showNotification(
                                it,
                                "Smart Lock 🏠",
                                friendlyMessage
                            )
                        }

                        handleMqttLog(payload)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client.connect(options)

            // 🔥 SUBSCRIBE CẢ STATUS + LOG
            client.subscribe("lock/log", 1)
            client.subscribe("lock/status", 1)

            startOfflineWatcher()

            Log.d(TAG, "✅ MQTT connected & subscribed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ MQTT connect error", e)
        }
    }

    /* ================= OFFLINE WATCHER ================= */

    private fun startOfflineWatcher() {
        scope.launch {
            while (isActive) {
                if (lastMessageTime != 0L) {
                    val now = System.currentTimeMillis()
                    if (now - lastMessageTime > OFFLINE_TIMEOUT) {
                        _deviceOnlineFlow.value = false
                    }
                }
                delay(2000)
            }
        }
    }

    /* ================= FRIENDLY MESSAGE ================= */

    private fun getEasyMessage(payload: String): String {
        return try {
            val obj = Json.parseToJsonElement(payload).jsonObject
            val event = obj["event"]?.jsonPrimitive?.content ?: ""
            val method = obj["method"]?.jsonPrimitive?.content ?: ""

            when {
                event == "UNLOCK" && method == "APP" ->
                    "🔓 Cửa đã mở từ xa bằng điện thoại"

                event == "UNLOCK" && method == "PASSWORD" ->
                    "🔑 Cửa đã mở bằng mật khẩu"

                event == "UNLOCK" && method == "RFID" ->
                    "🪪 Có người vừa quét thẻ vào nhà"

                event == "DENIED" ->
                    "⚠️ Cảnh báo: Có người cố mở cửa trái phép!"

                event == "ADD_RFID" ->
                    "🆕 Đã thêm thẻ RFID mới"

                else ->
                    "Hoạt động mới: $event"
            }
        } catch (e: Exception) {
            "Phát hiện hoạt động mới tại cửa"
        }
    }

    /* ================= HANDLE LOG ================= */

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleMqttLog(json: String) {
        scope.launch {
            try {
                val obj = Json.parseToJsonElement(json).jsonObject
                val event = obj["event"]?.jsonPrimitive?.content ?: "UNKNOWN"
                val uid = obj["uid"]?.jsonPrimitive?.content

                val log = DoorLog(
                    id = UUID.randomUUID().toString(),
                    action = event,
                    method = obj["method"]?.jsonPrimitive?.content ?: "UNKNOWN",
                    rfid_uid = uid,
                    created_at = Instant.now().toString()
                )

                SupabaseClient.client.from("door_logs").insert(log)

                if (event == "ADD_RFID" && uid != null) {
                    val newUser = mapOf(
                        "uid" to uid,
                        "name" to "Thẻ mới ($uid)",
                        "created_at" to Instant.now().toString()
                    )
                    SupabaseClient.client.from("rfid_users").upsert(newUser)
                    _rfidScanFlow.emit(uid)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Handle MQTT log error", e)
            }
        }
    }

    /* ================= COMMANDS ================= */

    fun openDoor() {
        if (!_deviceOnlineFlow.value) {
            Log.e(TAG, "❌ ESP32 OFFLINE – không thể mở cửa")
            return
        }

        if (::client.isInitialized && client.isConnected) {
            val message =
                MqttMessage("""{"command":"UNLOCK","method":"APP"}""".toByteArray())
            client.publish("lock/cmd", message)
        }
    }

    fun deleteRfid(uid: String): Boolean {

        if (!_deviceOnlineFlow.value) {
            Log.e(TAG, "❌ ESP32 OFFLINE – không thể xóa RFID")
            return false
        }

        if (::client.isInitialized && client.isConnected) {
            val payload = """{"command":"DELETE_RFID","uid":"$uid"}"""
            client.publish("lock/cmd", MqttMessage(payload.toByteArray()))
            return true
        }

        return false
    }

}
