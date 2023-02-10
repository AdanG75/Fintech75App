package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentMovementOptionsBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity

class MovementOptionsFragment : Fragment(R.layout.fragment_movement_options) {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentMovementOptionsBinding
    private lateinit var buttonSettings: ImageButton

    private lateinit var currentUser: UserCredential


    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMovementOptionsBinding.bind(view)
        buttonSettings = binding.bSettings

        setup()
    }

    private fun setup() {
        (activity as MainActivity).showBottomNavigation()

        currentUserListener()
        setStateSettingsButton(true)

        // payButton
        binding.bPay.setOnClickListener {
            Log.d(fragmentName, "Go to pay screen...")
            val action = MovementOptionsFragmentDirections.actionMovementsFragmentToPayFragment(
                currentCreditId = -1,
                currentCreditType = "N/A",
                marketId = "N/A"
            )
            findNavController().navigate(action)
        }
        binding.bPay.isEnabled = true

        // depositButton
        binding.bDeposit.setOnClickListener {
            Log.d(fragmentName, "Go to deposit screen...")
            val action = MovementOptionsFragmentDirections.actionMovementsFragmentToDepositFragment(
                destinationCreditId = -1,
                destinationCreditType = "N/A"
            )
            findNavController().navigate(action)
        }
        binding.bDeposit.isEnabled = true

        // transferButton
        binding.bTransfer.setOnClickListener {
            Log.d(fragmentName, "Go to transfer screen...")
            val action = MovementOptionsFragmentDirections.actionMovementsFragmentToTransferFragment(
                originCreditId = -1,
                originCreditType = "N/A"
            )
            findNavController().navigate(action)
        }
        binding.bTransfer.isEnabled = true

        // withdrawButton
        binding.bWithdraw.setOnClickListener {
            Log.d(fragmentName, "Go to withdraw screen...")
            val action = MovementOptionsFragmentDirections.actionMovementsFragmentToWithdrawFragment(
                originCreditId = -1,
                originCreditType = "N/A"
            )
            findNavController().navigate(action)
        }
        binding.bWithdraw.isEnabled = true
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A"
        )
        Log.d(fragmentName, "User ID: ${currentUser.userID}")
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = MovementOptionsFragmentDirections.actionMovementsFragmentToSettingsFragment(
                    typeUser = currentUser.typeUser,
                    token = currentUser.token
                )
                findNavController().navigate(action)
            }
        } else {
            buttonSettings.setOnClickListener {
                return@setOnClickListener
            }
        }
    }
}