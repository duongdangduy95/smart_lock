package com.example.smartlock.ui.rfid

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.R
import com.example.smartlock.data.model.RfidUser
import com.example.smartlock.data.repository.LockRepository
import com.example.smartlock.data.supabase.SupabaseClient
import com.example.smartlock.databinding.FragmentRfidBinding
import com.example.smartlock.mqtt.MqttManager
import com.example.smartlock.ui.logs.LogAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class RfidFragment : Fragment(R.layout.fragment_rfid) {

    // 1. Sửa: Xóa <Any?> để khớp với Repository mới đã sửa
    private val repo = LockRepository<Any>()

    private var _binding: FragmentRfidBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RfidAdapter
    private val rfidList = mutableListOf<RfidUser>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRfidBinding.bind(view)

        // 2. Sửa: Thay thế TODO() bằng logic gọi hàm xóa
        adapter = RfidAdapter(
            list = rfidList,
            onEdit = { user -> showEditNameDialog(user) },
            onDelete = { user -> deleteUserSync(user) },
            onItemClick = { user -> showUserLogsSheet(user) }
        )

        binding.rfidRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.rfidRecycler.adapter = adapter

        loadData()
    }

    // 3. Viết hàm load dữ liệu riêng để tái sử dụng
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Đảm bảo dùng getRfidUsers() nếu bạn đặt tên vậy trong Repo
                val users = repo.getRfidUsers()
                adapter.updateData(users)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showUserLogsSheet(user: RfidUser) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_user_logs_sheet, null)
        dialog.setContentView(view)

        val rvLogs = view.findViewById<RecyclerView>(R.id.rvUserLogs)
        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)

        tvTitle.text = "Lịch sử: ${user.name}"

        // Khởi tạo Adapter cho Log (Bạn có thể dùng chung LogAdapter đã có)
        val logAdapter = LogAdapter(mutableListOf())
        rvLogs.layoutManager = LinearLayoutManager(context)
        rvLogs.adapter = logAdapter

        // Load dữ liệu
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val logs = repo.getLogsByUid(user.uid)
                logAdapter.updateData(logs) // Đảm bảo hàm updateData trong LogAdapter nhận List<DoorLog>
            } catch (e: Exception) {
                Log.e("RFID", "Lỗi tải log: ${e.message}")
            }
        }

        dialog.show()
    }
    // 4. Hàm xử lý xóa đồng bộ Database và MQTT
    private fun deleteUserSync(user: RfidUser) {
        viewLifecycleOwner.lifecycleScope.launch {

            // 1️⃣ Gửi lệnh xóa cho ESP32
            val success = MqttManager.deleteRfid(user.uid)

            if (!success) {
                Toast.makeText(
                    requireContext(),
                    "❌ Thiết bị offline – không thể xóa thẻ",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            try {
                // 2️⃣ Xóa trong Supabase
                repo.deleteRfidUser(user.uid)

                Toast.makeText(
                    requireContext(),
                    "✅ Đã xóa ${user.name}",
                    Toast.LENGTH_SHORT
                ).show()

                loadData()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ ESP32 đã xóa nhưng DB lỗi",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }






    private fun showEditNameDialog(user: RfidUser) {
        // Đảm bảo EditNameDialog nhận tham số đúng (uid và repository)
        EditNameDialog(user.uid, repo) {
            loadData() // Callback tải lại dữ liệu sau khi đổi tên thành công
        }.show(parentFragmentManager, "edit_name")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}