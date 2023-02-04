package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.ClientProfile
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentProfileClientBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import java.security.PrivateKey

class ProfileClientFragment : Fragment(R.layout.fragment_profile_client) {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var currentUser: UserCredential
    private lateinit var binding: FragmentProfileClientBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var buttonSettings: ImageButton

    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileClientBinding.bind(view)
        screenLoading = binding.rlClientProfileLoading
        buttonSettings = binding.bSettings

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE
        (activity as MainActivity).showBottomNavigation()

        currentUserListener()
        userPrivateKeyListener()
        refreshListener()
        catchResultFromDialogs()
        setStateSettingsButton(true)

        getClientProfile()
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

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun getClientProfile() {
        // val pkMessage: String = userPrivateKey?.toString() ?: "null"
        // Log.d(fragmentName, "Private Key: $pkMessage")
        userPrivateKey?.let { privateKey ->
            userViewModel.fetchClientProfile(
                currentUser.token, currentUser.userID, currentUser.typeUser, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading client's profile...")
                        screenLoading.visibility = View.VISIBLE
                        setStateSettingsButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting client's profile has finished successfully")
                        val clientProfile: ClientProfile = result.data as ClientProfile
                        bindClientProfile(clientProfile)
                        setStateSettingsButton(true)
                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching client's profile. Please try again")
                        binding.srlRefresh.isRefreshing = false
                        showTryAgainFetchClientProfileDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Bad credentials")
                        binding.srlRefresh.isRefreshing = false
                        showInvalidCredentialsDialog()
                    }
                }
            }
        }
    }

    private fun bindClientProfile(client: ClientProfile) {
        binding.tvUserId.text = client.user.idUser.toString()
        "${client.user.name} ${client.client.lastName}".also {
            binding.tvUserName.text = it
        }
        binding.tvUserEmail.text = client.user.email
        binding.tvUserPhone.text = client.user.phone
        binding.tvUserBirthdate.text = client.client.birthDate
        binding.tvUserClientId.text = client.client.birthDate

        if (client.client.addresses.isEmpty()) {
            binding.cvAddress.visibility = View.GONE
        } else {
            val baseAddress = client.client.addresses[0]

            binding.tvAddressId.text = baseAddress.idAddress.toString()
            binding.tvAddressZipCode.text = baseAddress.zipCode
            binding.tvAddressState.text = baseAddress.state
            binding.tvAddressCity.text = baseAddress.city ?: "N/P"
            binding.tvAddressNeighborhood.text = baseAddress.neighborhood ?: "N/P"
            binding.tvAddressStreet.text = baseAddress.street ?: "N/P"
            binding.tvAddressOut.text = baseAddress.extNumber
            binding.tvAddressInner.text = baseAddress.innerNumber ?: "N/P"
        }

        if (client.client.fingerprints.isEmpty()) {
            binding.cvFingerprint.visibility = View.GONE
        } else {
            val baseFingerprint = client.client.fingerprints[0]
            binding.tvFingerprintName.text = baseFingerprint.aliasFingerprint
            binding.tvFingerprintDate.text = baseFingerprint.createdTime.substring(0, 10)
            binding.tvFingerprintId.text = baseFingerprint.idFingerprint
        }
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, true)
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = ProfileClientFragmentDirections
                    .actionProfileClientFragmentToSettingsFragment(currentUser.typeUser, currentUser.token)
                findNavController().navigate(action)
            }
        } else {
            buttonSettings.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun refreshListener() {
        Log.d(fragmentName, "Refreshing credits...")
        binding.srlRefresh.setOnRefreshListener {
            getClientProfile()
        }
    }

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvClientProfileMsg.text = getString(R.string.closing_session)
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Good bye")
                    goToLogin()
                }
                is Resource.TryAgain -> {
                    screenLoading.visibility = View.GONE
                    Log.d(fragmentName, "Un error ocurrió, favor de intentarlo de nuevo")
                    showLogoutFailedDialog()
                }
                is Resource.Failure -> goToLogin()
            }
        }
    }

    private fun catchResultFromDialogs() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("action")
            ?.observe(viewLifecycleOwner) { action ->
                when(action) {
                    "finishSession" -> goToLogin()
                    "clientProfile" -> getClientProfile()
                    AppConstants.ACTION_CLOSE_APP -> activity?.finish()
                    AppConstants.ACTION_CLOSE_SESSION -> logout()
                    else -> return@observe
                }
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("closed")
            ?.observe(viewLifecycleOwner) { closed ->
                when(closed) {
                    "none" -> return@observe
                    "close" -> activity?.finish()
                    "endSession" -> goToLogin()
                    else -> return@observe
                }
            }
    }

    private fun showInvalidCredentialsDialog() = showNotificationDialog(
        title = "Credenciales invalidas",
        message = "Las credenciales han expirado",
        bOkAction = "dismiss",
        bOkText = getString(R.string.try_again),
        bOkAvailable = false,
        bCancelAction = "finishSession",
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true,
        closeAction = "endSession"
    )

    private fun showLogoutFailedDialog() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cerrar la sesión",
        bOkAction = "logout",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    private fun showTryAgainFetchClientProfileDialog() = showNotificationDialog(
        title = "Perfil no cargado",
        message = "No se pudo cargar el perfil del usuario",
        bOkAction = "clientProfile",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean, closeAction: String = "none"
    ) {
        val action = ProfileClientFragmentDirections.actionProfileClientFragmentToNotificationDialogFragment(
            title = title,
            msg = message,
            bOkAction = bOkAction,
            bOkText = bOkText,
            bOkAvailable = bOkAvailable,
            bCancelAction = bCancelAction,
            bCancelText = bCancelText,
            bCancelAvailable = bCancelAvailable,
            closeAction = closeAction
        )
        findNavController().navigate(action)
    }
}