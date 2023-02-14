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
import com.example.fintech75.databinding.FragmentAddElementBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity

class AddElementFragment : Fragment(R.layout.fragment_add_element) {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentAddElementBinding
    private lateinit var buttonSettings: ImageButton

    private lateinit var currentUser: UserCredential

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddElementBinding.bind(view)
        buttonSettings = binding.bSettings

        setup()
    }

    private fun setup() {
        (activity as MainActivity).showBottomNavigation()

        currentUserListener()
        setStateSettingsButton(true)

        binding.bAddClient.setOnClickListener {
            Log.d(fragmentName, "Go to sign-up client fragment")
            val action = AddElementFragmentDirections.actionAddFragmentToSignUpClientFragment()
            findNavController().navigate(action)
        }
        binding.bAddClient.isEnabled = true

        binding.bAddCredit.setOnClickListener {
            Log.d(fragmentName, "Go to create credit fragment")
            val action = AddElementFragmentDirections.actionAddFragmentToCreateCreditFragment()
            findNavController().navigate(action)
        }
        binding.bAddCredit.isEnabled = true
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A",
            email = "N/A"
        )
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = AddElementFragmentDirections.actionAddFragmentToSettingsFragment(
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