package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentSettingsBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val fragmentName= this::class.java.toString()
    private val args: SettingsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var switchNotification: SwitchMaterial
    private lateinit var logoutButton: Button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)
        switchNotification = binding.sNotifications
        logoutButton = binding.bLogout
        screenLoading = binding.rlSettingsLoading

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.GONE

        setupSwitchButton()
        setupLogoutButton()
    }

    private fun setupSwitchButton() {
        switchNotification.isChecked = GlobalSettings.notify
        if (args.typeUser == "client") {
            switchNotification.visibility = View.VISIBLE
            switchNotification.setOnCheckedChangeListener { compoundButton, isChecked ->
                Log.d(fragmentName, "Is notification enable? : $isChecked")
                GlobalSettings.notify = isChecked
            }
        } else {
            GlobalSettings.notify = true
            binding.sNotifications.visibility = View.GONE
        }
    }

    private fun setupLogoutButton() {
        logoutButton.isEnabled = true
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        userViewModel.logout(args.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    Log.d(fragmentName, "Closing session...")
                    screenLoading.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Good bye")
                    goToLogin()
                }
                is Resource.TryAgain -> {
                    screenLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "No se pudo cerrar la sesión", Toast.LENGTH_SHORT).show()
                }
                is Resource.Failure -> goToLogin()
            }
        }
    }

    private fun goToLogin() {
        val action = SettingsFragmentDirections.actionSettingsFragmentToLoginFragment()
        findNavController().navigate(action)
    }
}