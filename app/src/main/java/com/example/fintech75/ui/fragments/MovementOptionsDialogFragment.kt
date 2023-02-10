package com.example.fintech75.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
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
        if (args.userType == "client") {
            setStateWithdrawButton(false)
            if (args.creditType == "local") {
                setStateTransferButton(false)
            } else {
                setStateTransferButton(true)
            }
            setStatePayButton(true)
            setStateDepositButton(true)
        } else {
            setStateWithdrawButton(true)
            setStateTransferButton(true)
            setStatePayButton(true)
            setStateDepositButton(true)
        }
    }

    private fun setStatePayButton(isEnable: Boolean = true) {
        binding.bPay.isEnabled = isEnable
        if (isEnable) {
            binding.bPay.visibility = View.VISIBLE
            binding.bPay.setOnClickListener {
                Log.d(fragmentName, "Go to pay screen...")
                val action = MovementOptionsDialogFragmentDirections
                    .actionMovementOptionsDialogFragmentToPayFragment(
                        currentCreditId = args.creditId,
                        currentCreditType = args.creditType,
                        marketId = "N/A"
                    )
                findNavController().navigate(action)
            }
        } else {
            binding.bPay.visibility = View.GONE
        }
    }

    private fun setStateDepositButton(isEnable: Boolean = true) {
        binding.bDeposit.isEnabled = isEnable
        if (isEnable) {
            binding.bDeposit.visibility = View.VISIBLE
            binding.bDeposit.setOnClickListener {
                Log.d(fragmentName, "Go to deposit screen...")
                val action = MovementOptionsDialogFragmentDirections
                    .actionMovementOptionsDialogFragmentToDepositFragment(
                        destinationCreditId = args.creditId,
                        destinationCreditType = args.creditType
                    )
                findNavController().navigate(action)
            }
        } else {
            binding.bDeposit.visibility = View.GONE
        }
    }

    private fun setStateTransferButton(isEnable: Boolean = true) {
        binding.bTransfer.isEnabled = isEnable
        if (isEnable) {
            binding.bTransfer.visibility = View.VISIBLE
            binding.bTransfer.setOnClickListener {
                Log.d(fragmentName, "Go to transfer screen...")
                val action = MovementOptionsDialogFragmentDirections
                    .actionMovementOptionsDialogFragmentToTransferFragment(
                        originCreditId = args.creditId,
                        originCreditType = args.creditType
                    )
                findNavController().navigate(action)
            }
        } else {
            binding.bTransfer.visibility = View.GONE
        }
    }

    private fun setStateWithdrawButton(isEnable: Boolean = true) {
        binding.bWithdraw.isEnabled = isEnable
        if (isEnable) {
            binding.bWithdraw.visibility = View.VISIBLE
            binding.bWithdraw.setOnClickListener {
                Log.d(fragmentName, "Go to withdraw screen...")
                val action = MovementOptionsDialogFragmentDirections
                    .actionMovementOptionsDialogFragmentToWithdrawFragment(
                        originCreditId = args.creditId,
                        originCreditType = args.creditType
                    )
                findNavController().navigate(action)
            }
        } else {
            binding.bWithdraw.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}