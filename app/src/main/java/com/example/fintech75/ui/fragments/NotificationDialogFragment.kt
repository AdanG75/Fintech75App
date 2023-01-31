package com.example.fintech75.ui.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentNotificationDialogBinding

class NotificationDialogFragment : DialogFragment(R.layout.fragment_notification_dialog) {
    private val fragmentName = this::class.java.toString()
    private val args: NotificationDialogFragmentArgs by navArgs()

    private lateinit var binding: FragmentNotificationDialogBinding
    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button
    private lateinit var titleText: TextView
    private lateinit var msgText: TextView

    private var clicked: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotificationDialogBinding.bind(view)
        acceptButton = binding.bDialogAccept
        cancelButton = binding.bDialogClose
        titleText = binding.tvDialogTitle
        msgText = binding.tvDialogMsg

        titleText.text = args.title
        msgText.text = args.msg

        if (args.bOkAvailable) {
            acceptButton.isEnabled = true
            acceptButton.text = args.bOkText
            Log.d(fragmentName, "Action: ${args.bOkAction}")
            acceptButton.setOnClickListener {
                clicked = true
                findNavController().previousBackStackEntry?.savedStateHandle?.set("action", args.bOkAction)
                dismiss()
            }
        } else {
            acceptButton.isEnabled = false
            acceptButton.visibility = View.INVISIBLE
        }

        if (args.bCancelAvailable) {
            cancelButton.isEnabled = true
            cancelButton.text = args.bCancelText
            Log.d(fragmentName, "Action: ${args.bCancelAction}")
            cancelButton.setOnClickListener {
                clicked = true
                findNavController().previousBackStackEntry?.savedStateHandle?.set("action", args.bCancelAction)
                dismiss()
            }
        } else {
            cancelButton.isEnabled = false
            cancelButton.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val closeAction = if (clicked) {
            "none"
        } else {
            args.closeAction
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set("closed", closeAction)
    }
}