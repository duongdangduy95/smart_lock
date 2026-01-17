package com.example.smartlock.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.data.model.DoorLog
import com.example.smartlock.data.model.RfidUser
import com.example.smartlock.databinding.ItemLogBinding
import com.example.smartlock.util.DateUtil

class LogAdapter(
    private val list: MutableList<DoorLog>
) : RecyclerView.Adapter<LogAdapter.VH>() {

    inner class VH(val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root)
    private var userMap: Map<String, String> = emptyMap()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val log = list[position]

        holder.binding.txtMethod.text = "Phương thức: ${log.method}"
        holder.binding.txtTime.text =
            DateUtil.formatToVN(log.created_at)

        holder.binding.txtAction.text = "Hành động: ${log.action}"

        // Logic hiển thị: Tên người dùng + UID
        if (log.rfid_uid != null) {
            val uid = log.rfid_uid
            val name = userMap[uid] // Tra cứu tên từ Map

            if (name != null) {
                // Nếu tìm thấy tên trong bảng rfid_users
                holder.binding.txtUser.text = "Người dùng: $name\n(Mã thẻ: $uid)"
            } else {
                // Nếu thẻ này chưa được đặt tên (chưa có trong bảng rfid_users)
                holder.binding.txtUser.text = "Mã thẻ chưa đăng ký: $uid"
            }
        } else {
            // Trường hợp mở bằng APP hoặc Mật khẩu
            holder.binding.txtUser.text = "Mở cửa qua: ${log.method ?: "Hệ thống"}"
        }
    }
    fun updateData(newList: List<DoorLog>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
    fun setUserMap(users: List<RfidUser>) {
        // Bây giờ 'it' sẽ được hiểu là đối tượng RfidUser
        this.userMap = users.associate { it.uid to (it.name ?: "Không tên") }
        notifyDataSetChanged()
    }
    fun submit(newList: List<DoorLog>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
