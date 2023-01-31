package com.example.fintech75.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentLogoutDialogBinding

class LogoutDialogFragment : DialogFragment(R.layout.fragment_logout_dialog) {
    private val fragmentName = this::class.java.toString()

    private lateinit var binding: FragmentLogoutDialogBinding
    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLogoutDialogBinding.bind(view)
        acceptButton = binding.bDialogFinish
        cancelButton = binding.bDialogCancel

        acceptButton.setOnClickListener {
            Log.d(fragmentName, "Result: true")
            findNavController().previousBackStackEntry?.savedStateHandle?.set("result", true)
            dismiss()
        }

        cancelButton.setOnClickListener {
            Log.d(fragmentName, "Result: false")
            findNavController().previousBackStackEntry?.savedStateHandle?.set("result", false)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}