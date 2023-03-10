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
import com.example.fintech75.data.model.MarketProfile
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentProfileMarketBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import java.security.PrivateKey

class ProfileMarketFragment : Fragment(R.layout.fragment_profile_market) {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        )
        )
    }

    private lateinit var currentUser: UserCredential
    private lateinit var binding: FragmentProfileMarketBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var buttonSettings: ImageButton

    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileMarketBinding.bind(view)
        screenLoading = binding.rlMarketProfileLoading
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

        getMarketProfile()
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A",
            email = "N/A"
        )
        Log.d(fragmentName, "User ID: ${currentUser.userID}")
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun getMarketProfile() {
        userPrivateKey?.let { privateKey ->
            userViewModel.fetchMarketProfile(
                currentUser.token, currentUser.userID, currentUser.typeUser, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading market's profile...")
                        screenLoading.visibility = View.VISIBLE
                        setStateSettingsButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting market's profile has finished successfully")
                        val marketProfile: MarketProfile = result.data as MarketProfile
                        bindMarketProfile(marketProfile)
                        setStateSettingsButton(true)
                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching market's profile. Please try again")
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

    private fun bindMarketProfile(market: MarketProfile) {
        binding.tvUserId.text = market.user.idUser.toString()
        binding.tvUserName.text = market.user.name
        binding.tvUserEmail.text = market.user.email
        binding.tvUserPhone.text = market.user.phone
        binding.tvUserWebPage.text = market.market.webPage ?: "N/P"
        binding.tvUserMarketId.text = market.market.idMarket

        if (market.market.branches.isEmpty()) {
            binding.cvBranch.visibility = View.GONE
        } else {
            val branchBase = market.market.branches[0]
            binding.tvBranchName.text = branchBase.branchName
            binding.tvBranchHours.text = branchBase.serviceHours
            binding.tvBranchPhone.text = branchBase.phone ?: "N/P"

            binding.tvAddressZipCode.text = branchBase.address.zipCode
            binding.tvAddressState.text = branchBase.address.state
            binding.tvAddressCity.text = branchBase.address.city ?: "N/P"
            binding.tvAddressNeighborhood.text = branchBase.address.neighborhood ?: "N/P"
            binding.tvAddressStreet.text = branchBase.address.street ?: "N/P"
            binding.tvAddressOut.text = branchBase.address.extNumber
            binding.tvAddressInner.text = branchBase.address.innerNumber ?: "N/P"
        }

        if (market.user.accounts.isEmpty()) {
            binding.cvAccount.visibility = View.GONE
        } else {
            val baseAccount = market.user.accounts[0]
            binding.tvAccountName.text = baseAccount.aliasAccount
            binding.tvAccountEmail.text = baseAccount.paypalEmail
            binding.tvAccountId.text = baseAccount.idAccount.toString()
        }

        binding.tvOutstandingId.text = market.outstanding_payment.idOutstanding.toString()
        binding.tvOutstandingAmount.text = market.outstanding_payment.amount
        binding.tvOutstandingLast.text = market.outstanding_payment.lastCashClosing?.substring(0, 10) ?: "N/A"
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = ProfileMarketFragmentDirections
                    .actionProfileMarketFragmentToSettingsFragment(currentUser.typeUser, currentUser.token)
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
            getMarketProfile()
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
                    binding.tvMarketProfileMsg.text = getString(R.string.closing_session)
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Good bye")
                    goToLogin()
                }
                is Resource.TryAgain -> {
                    screenLoading.visibility = View.GONE
                    Log.d(fragmentName, "Un error ocurri??, favor de intentarlo de nuevo")
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
                    "marketProfile" -> getMarketProfile()
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
        message = "No se pudo cerrar la sesi??n",
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
        bOkAction = "marketProfile",
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
        val action = ProfileMarketFragmentDirections.actionProfileMarketFragmentToNotificationDialogFragment(
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