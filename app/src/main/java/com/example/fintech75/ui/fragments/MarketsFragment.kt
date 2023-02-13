package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.application.ItemClickListener
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.CreditBase
import com.example.fintech75.data.model.MarketFull
import com.example.fintech75.data.model.MarketsList
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentMarketsBinding
import com.example.fintech75.presentation.MarketViewModel
import com.example.fintech75.presentation.MarketViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MarketRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.example.fintech75.ui.adapters.MarketAdapter
import java.security.PrivateKey

class MarketsFragment : Fragment(R.layout.fragment_markets), ItemClickListener {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val marketViewModel: MarketViewModel by viewModels {
        MarketViewModelFactory( MarketRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentMarketsBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var buttonSettings: ImageButton

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMarketsBinding.bind(view)
        screenLoading = binding.rlMarketsLoading
        buttonSettings = binding.bSettings

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()
        refreshListener()
        catchResultFromDialogs()
        setStateSettingsButton(true)

        getMarkets()
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

    private fun refreshListener() {
        Log.d(fragmentName, "Refreshing markets...")
        binding.srlRefresh.setOnRefreshListener {
            getMarkets()
        }
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = MarketsFragmentDirections
                    .actionMarketsFragmentToSettingsFragment(currentUser.typeUser, currentUser.token)
                findNavController().navigate(action)
            }
        } else {
            buttonSettings.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun getMarkets() {
        userPrivateKey?.let { privateKey ->
            marketViewModel.fetchAllMarkets(
                currentUser.token, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading markets...")
                        screenLoading.visibility = View.VISIBLE
                        setStateSettingsButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting markets have finished successfully")
                        val marketList: MarketsList = result.data as MarketsList
                        binding.rvMarkets.adapter = MarketAdapter(marketList.markets, this)
                        // Log.d(fragmentName, marketList.markets[0].user.name)

                        if (marketList.markets.isEmpty()) {
                            binding.rvMarkets.visibility = View.GONE
                            binding.cvWithoutMarkets.visibility = View.VISIBLE
                        } else {
                            binding.rvMarkets.visibility = View.VISIBLE
                            binding.cvWithoutMarkets.visibility = View.GONE
                        }

                        setStateSettingsButton(true)
                        (activity as MainActivity).showBottomNavigation()
                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching markets. Please try again")
                        binding.srlRefresh.isRefreshing = false
                        showTryAgainFetchAllMarketsDialog()
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
                    binding.tvMarketsMsg.text = getString(R.string.closing_session)
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
                    "getMarkets" -> getMarkets()
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

    private fun showTryAgainFetchAllMarketsDialog() = showNotificationDialog(
        title = "Tiendas no cargadas",
        message = "No se pudo cargar las tiendas registradas",
        bOkAction = "getMarkets",
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
        val action = MarketsFragmentDirections.actionMarketsFragmentToNotificationDialogFragment(
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

    override fun onCreditClick(credit: CreditBase) {
        return Unit
    }

    override fun onMarketClick(market: MarketFull) {
        Log.d(fragmentName, "Go to market detail")
        val action = MarketsFragmentDirections.actionMarketsFragmentToMarketDetailFragment(
            marketName = market.user.name,
            marketEmail = market.user.email,
            marketPhone = market.user.phone,
            marketId = market.market.idMarket
        )
        findNavController().navigate(action)
    }
}