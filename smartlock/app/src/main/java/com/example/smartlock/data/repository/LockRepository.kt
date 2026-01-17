package com.example.smartlock.data.repository

import com.example.smartlock.data.model.DoorLog
import com.example.smartlock.data.model.RfidUser //
import com.example.smartlock.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * Repository quản lý dữ liệu từ Supabase cho khóa thông minh.
 * Đã loại bỏ Generic <T> để tránh lỗi Reified Type Parameter.
 */
class LockRepository<T> {

    private val client = SupabaseClient.client

    // --- PHẦN NHẬT KÝ (DOOR LOGS) ---

    /**
     * Lấy danh sách nhật ký từ bảng "door_logs", sắp xếp mới nhất lên đầu.
     */
    suspend fun getLogs(s: String): List<DoorLog> {
        return client
            .from("door_logs")
            .select {
                order(column = "created_at", order = Order.DESCENDING)
            }
            .decodeList<DoorLog>()
    }

    // --- PHẦN NGƯỜI DÙNG RFID (RFID USERS) ---

    /**
     * Lấy danh sách người dùng từ bảng "rfid_users".
     */
    suspend fun getRfidUsers(): List<RfidUser> {
        return client
            .from("rfid_users")
            .select()
            .decodeList<RfidUser>()
    }

    /**
     * Thêm một người dùng RFID mới vào Database.
     */
    suspend fun addRfidUser(user: RfidUser) {
        client.from("rfid_users").insert(user)
    }

    /**
     * Cập nhật tên hiển thị cho một mã UID cụ thể.
     * Dùng cho chức năng nút "Sửa" trong danh sách RFID.
     */
    suspend fun updateName(uid: String, newName: String) {
        client.from("rfid_users").update(
            {
                set("name", newName) // Cập nhật cột 'name' trong Supabase
            }
        ) {
            filter { eq("uid", uid) } // Lọc theo mã thẻ UID
        }
    }
    suspend fun getLogsByUid(uid: String): List<DoorLog> {
        return client.from("door_logs")
            .select {
                filter {
                    eq("rfid_uid", uid) // Lọc chính xác theo UID thẻ
                }
            }.decodeList<DoorLog>()
            .sortedByDescending { it.created_at } // Hiện cái mới nhất lên đầu
    }
    /**
     * Xóa người dùng khỏi bảng "rfid_users" dựa trên mã UID.
     * Dùng cho chức năng nút "Xóa" trong danh sách RFID.
     */
    suspend fun deleteRfidUser(uid: String) {
        client.from("rfid_users").delete {
            filter { eq("uid", uid) }
        }
    }


}