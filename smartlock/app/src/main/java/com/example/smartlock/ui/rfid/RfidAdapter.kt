package com.example.smartlock.ui.rfid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.data.model.RfidUser //
import com.example.smartlock.databinding.ItemRfidBinding //

class RfidAdapter(
    private val list: MutableList<RfidUser>,
    // Xóa repo khỏi đây, thay bằng callback để Fragment xử lý (nhằm kết hợp với lệnh MQTT)
    private val onEdit: (RfidUser) -> Unit,
    private val onDelete: (RfidUser) -> Unit,
    private val onItemClick: (RfidUser) -> Unit
//    repo: LockRepository<Any?>
) : RecyclerView.Adapter<RfidAdapter.VH>() {

    inner class VH(val binding: ItemRfidBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRfidBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = list[position]

        // Hiển thị thông tin từ bảng rfid_users
        holder.binding.txtName.text = user.name ?: "Chưa đặt tên"
        holder.binding.txtUid.text = "Mã RFID: ${user.uid}"

        // Hiển thị ngày tháng (cắt lấy 10 ký tự đầu yyyy-MM-dd)
        holder.binding.txtDate.text = "Ngày thêm: ${user.created_at?.take(10) ?: "N/A"}"
        holder.itemView.setOnClickListener {
            onItemClick(user) // Kích hoạt hàm showUserLogsSheet từ Fragment
        }
        // Xử lý nút Xóa: Gửi tín hiệu ra Fragment để xóa cả DB và gửi lệnh MQTT
        holder.binding.btnDelete.setOnClickListener {
            onDelete(user)
        }

        // Xử lý nút Sửa: Mở Dialog đổi tên
        holder.binding.btnEdit.setOnClickListener {
            onEdit(user)
        }
    }

    // Sửa kiểu dữ liệu từ Unit thành List<RfidUser>
    fun updateData(newList: List<RfidUser>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}

private fun <E> List<E>.addAll(newList: Unit) {
    TODO("Not yet implemented")
}
