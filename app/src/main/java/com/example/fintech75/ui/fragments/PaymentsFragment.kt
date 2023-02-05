package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.Payments
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentPaymentsBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.example.fintech75.ui.adapters.MovementAdapter
import java.security.PrivateKey

class PaymentsFragment : Fragment(R.layout.fragment_payments) {
    private val fragmentName = this::class.java.toString()
    private val args: PaymentsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentPaymentsBinding
    private lateinit var screenLoading: RelativeLayout

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPaymentsBinding.bind(view)
        screenLoading = binding.rlPaymentsLoading

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()
        refreshListener()
        catchResultFromDialogs()

        getUserPayments()
    }


    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A"
        )
        if (currentUser.userID != args.idUser) {
            showInvalidCredentialsDialog()
        }

        Log.d(fragmentName, "User ID: ${currentUser.userID}")
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun refreshListener() {
        Log.d(fragmentName, "Refreshing credits...")
        binding.srlRefresh.setOnRefreshListener {
            getUserPayments()
        }
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun getUserPayments() {
        userPrivateKey?.let { privateKey ->
            userViewModel.fetchPaymentsUser(
                currentUser.token, currentUser.userID, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading user's payments...")
                        screenLoading.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting user's payments have finished successfully")
                        val userPayments: Payments = result.data as Payments
                        binding.rvPayments.adapter = MovementAdapter(userPayments.payments)

                        if (userPayments.payments.isEmpty()) {
                            binding.rvPayments.visibility = View.GONE
                            binding.cvWithoutPayments.visibility = View.VISIBLE
                        } else {
                            binding.rvPayments.visibility = View.VISIBLE
                            binding.cvWithoutPayments.visibility = View.GONE
                        }

                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching client's profile. Please try again")
                        binding.srlRefresh.isRefreshing = false
                        showTryAgainFetchPaymentsUserDialog()
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

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvPaymentsMsg.text = getString(R.string.closing_session)
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
                    "paymentsUser" -> getUserPayments()
                    "logout" -> logout()
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

    private fun showTryAgainFetchPaymentsUserDialog() = showNotificationDialog(
        title = "Pagos no cargados",
        message = "No se pudo cargar los pagos del usuario",
        bOkAction = "paymentsUser",
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
        val action = PaymentsFragmentDirections.actionPaymentsFragmentToNotificationDialogFragment(
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