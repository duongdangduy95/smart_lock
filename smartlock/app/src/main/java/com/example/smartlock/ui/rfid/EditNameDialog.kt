package com.example.smartlock.ui.rfid

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import com.example.smartlock.data.repository.LockRepository
import com.example.smartlock.databinding.DialogEditNameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditNameDialog(
    private val uid: String,
    private val repo: LockRepository<Any>,
    function: () -> Unit
) : DialogFragment() {

    private var _binding: DialogEditNameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditNameBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        binding.btnSave.setOnClickListener {
            val newName = binding.edtName.text.toString().trim()
            if (newName.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    repo.updateName(uid, newName)
                }
                dismiss()
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
