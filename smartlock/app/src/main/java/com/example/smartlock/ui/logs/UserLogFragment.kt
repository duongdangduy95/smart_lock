package com.example.smartlock.ui.logs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.R
import com.example.smartlock.data.model.DoorLog
import com.example.smartlock.data.model.RfidUser //
import com.example.smartlock.data.repository.LockRepository
import com.example.smartlock.databinding.FragmentUserLogBinding
import com.example.smartlock.mqtt.MqttManager
import kotlinx.coroutines.launch

class UserLogFragment : Fragment(R.layout.fragment_user_log) {

    private var _binding: FragmentUserLogBinding? = null
    private val binding get() = _binding!!

    // Repository cho nhật ký cửa
    private val logRepo = LockRepository<DoorLog>()

    // Repository cho người dùng RFID để tra cứu tên
    private val userRepo = LockRepository<RfidUser>()

    private val adapter = LogAdapter(mutableListOf())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserLogBinding.bind(view)

        binding.btnUnlock.setOnClickListener {
            MqttManager.openDoor()
            Toast.makeText(requireContext(), "Đang gửi lệnh mở khoá...", Toast.LENGTH_SHORT).show()
        }

        // Cấu hình danh sách hiển thị
        binding.logRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.logRecycler.adapter = adapter

        // Tải dữ liệu đồng bộ giữa 2 bảng: door_logs và rfid_users
        loadData()
    }

    /**
     * Hàm nạp dữ liệu từ Database
     */
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Bước 1: Lấy danh sách người dùng từ bảng rfid_users
                // Điều này giúp Adapter biết UID nào ứng với tên nào
                val users: List<RfidUser> = userRepo.getRfidUsers()
                adapter.setUserMap(users)

                // Bước 2: Lấy danh sách nhật ký cửa
                val logs: List<DoorLog> = logRepo.getLogs("")

                if (logs.isNotEmpty()) {
                    adapter.submit(logs)
                } else {
                    Log.d("UserLogFragment", "Dữ liệu nhật ký rỗng")
                }
            } catch (e: Exception) {
                Log.e("UserLogFragment", "Lỗi khi nạp dữ liệu: ${e.message}", e)
                Toast.makeText(requireContext(), "Không thể tải nhật ký", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}