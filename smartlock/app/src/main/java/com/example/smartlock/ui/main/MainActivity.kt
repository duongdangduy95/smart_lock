package com.example.smartlock.ui.main

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smartlock.R
import com.example.smartlock.mqtt.MqttManager
import com.example.smartlock.ui.logs.UserLogFragment
import com.example.smartlock.ui.rfid.RfidFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var txtDeviceStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ====== VIEW ======
        txtDeviceStatus = findViewById(R.id.txtDeviceStatus)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // ====== FRAGMENT MẶC ĐỊNH ======
        if (savedInstanceState == null) {
            replaceFragment(UserLogFragment())
            bottomNav.selectedItemId = R.id.nav_log
        }

        // ====== BOTTOM NAV ======
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_log -> {
                    replaceFragment(UserLogFragment())
                    true
                }
                R.id.nav_rfid -> {
                    replaceFragment(RfidFragment())
                    true
                }
                else -> false
            }
        }

        // ====== LẮNG NGHE TRẠNG THÁI ESP32 (ONLINE / OFFLINE) ======
        lifecycleScope.launch {
            MqttManager.deviceOnlineFlow.collectLatest { isOnline ->
                if (isOnline) {
                    showOnline()
                } else {
                    showOffline()
                }
            }
        }

        // ====== KẾT NỐI MQTT ======
        MqttManager.connect(this)
    }

    // ====== UI STATUS ======

    private fun showOnline() {
        txtDeviceStatus.text = "🟢 ESP32 Online"
        txtDeviceStatus.setBackgroundColor(Color.parseColor("#388E3C"))
    }

    private fun showOffline() {
        txtDeviceStatus.text = "🔴 ESP32 Offline"
        txtDeviceStatus.setBackgroundColor(Color.parseColor("#D32F2F"))
    }

    // ====== FRAGMENT REPLACE ======

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.container, fragment)
            .commit()
    }
}
