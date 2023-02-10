package com.example.fintech75.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentMovementOptionsDialogBinding

class MovementOptionsDialogFragment : DialogFragment(R.layout.fragment_movement_options_dialog) {
    private val fragmentName = this::class.java.toString()
    private val args: MovementOptionsDialogFragmentArgs by navArgs()

    private lateinit var binding: FragmentMovementOptionsDialogBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMovementOptionsDialogBinding.bind(view)

        setup()
    }

    private fun setup() {
        TODO("Without destination fragments")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}