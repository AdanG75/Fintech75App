package com.example.fintech75.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.CreditMarketClient
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentMarketDetailBinding
import com.example.fintech75.presentation.MarketViewModel
import com.example.fintech75.presentation.MarketViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MarketRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import java.security.PrivateKey

class MarketDetailFragment : Fragment(R.layout.fragment_market_detail) {
    private val fragmentName = this::class.java.toString()
    private val args: MarketDetailFragmentArgs by navArgs()
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
    private lateinit var binding: FragmentMarketDetailBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var payButton: Button

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMarketDetailBinding.bind(view)
        screenLoading = binding.rlMarketDetailLoading
        payButton = binding.bMarketPay

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        if (args.marketId == "N/A") {
            showNoMarketPassedDialog()
        }

        currentUserListener()
        userPrivateKeyListener()
        refreshListener()
        catchResultFromDialogs()
        setStatePayButton(false)

        getMarketDetail()
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

    private fun refreshListener() {
        Log.d(fragmentName, "Refreshing markets...")
        binding.srlRefresh.setOnRefreshListener {
            getMarketDetail()
        }
    }

    private fun setStatePayButton(isEnable: Boolean) {
        payButton.isEnabled = isEnable

        if (isEnable) {
            payButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            payButton.setOnClickListener {
                // val action = MarketDetailFragmentDirections.
                // findNavController().navigate(action)
            }
        } else {
            payButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            payButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun getMarketDetail() {
        userPrivateKey?.let { privateKey ->
            marketViewModel.fetchMarketDetail(
                currentUser.token, args.marketId, currentUser.userID, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading market's detail...")
                        screenLoading.visibility = View.VISIBLE
                        setStatePayButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting market's detail have finished successfully")
                        val marketBasedClient: CreditMarketClient = result.data as CreditMarketClient
                        Log.d(fragmentName, "Market type: ${marketBasedClient.market.typeMarket}")

                        bind(marketBasedClient)

                        setStatePayButton(true)
                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching market's detail. Please try again")
                        binding.srlRefresh.isRefreshing = false
                        showTryAgainMarketDetailDialog()
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

    private fun bind(marketBasedClient: CreditMarketClient) {
        binding.tvMarketName.text = args.marketName
        binding.tvMarketId.text = args.marketId

        if (args.marketEmail != "N/A" && Patterns.EMAIL_ADDRESS.matcher(args.marketEmail).matches()) {
            binding.tvMarketEmail.text = args.marketEmail
            binding.tvMarketEmail.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${args.marketEmail}")
                }
                startActivity(intent)
            }
        } else {
            binding.tvMarketEmail.text = "El establecimiento no tiene un email registrado"
        }

        if (args.marketPhone != "N/A" && Patterns.PHONE.matcher(args.marketPhone).matches()) {
            binding.tvMarketPhone.text = args.marketPhone
            binding.tvMarketPhone.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${args.marketPhone}")
                }
                startActivity(intent)
            }
        } else {
            binding.tvMarketPhone.text = "El establecimiento no tiene teléfono registrado"
        }

        if (marketBasedClient.market.webPage != null) {
            if (Patterns.WEB_URL.matcher(marketBasedClient.market.webPage).matches()) {
                binding.tvMarketWeb.text = marketBasedClient.market.webPage
                binding.tvMarketWeb.setOnClickListener {
                    val uri = Uri.parse(marketBasedClient.market.webPage)
                    val launchBrowser = Intent(Intent.ACTION_VIEW, uri)

                    startActivity(launchBrowser)
                }
            } else {
                binding.tvMarketWeb.text = "El establecimiento no tiene página de Internet registrada"
            }
        } else {
            binding.tvMarketWeb.text = "El establecimiento no tiene página de Internet registrada"
        }

        if (marketBasedClient.market.branches.isNotEmpty()) {
            val baseBranch = marketBasedClient.market.branches[0]
            binding.tvBranchName.text = baseBranch.branchName
            binding.tvBranchHours.text = baseBranch.serviceHours
            binding.tvBranchPhone.text = baseBranch.phone ?: "La sucursal no tiene teléfono registrado"

            binding.tvAddressZipCode.text = baseBranch.address.zipCode
            binding.tvAddressCity.text = baseBranch.address.city ?: "N/A"
            binding.tvAddressState.text = baseBranch.address.state
            binding.tvAddressNeighborhood.text = baseBranch.address.neighborhood ?: "N/A"
            binding.tvAddressStreet.text = baseBranch.address.street ?: "N/A"
            binding.tvAddressOut.text = baseBranch.address.extNumber
            binding.tvAddressInner.text = baseBranch.address.innerNumber ?: "N/A"
        } else {
            binding.cvBranch.visibility = View.GONE
        }
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvMarketDetailMsg.text = getString(R.string.closing_session)
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
                    "getMarketDetail" -> getMarketDetail()
                    "logout" -> logout()
                    "return" -> findNavController().popBackStack(R.id.markets_fragment, false)
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

    private fun showTryAgainMarketDetailDialog() = showNotificationDialog(
        title = "Tiendas no cargada",
        message = "No se pudo cargar el detalle de la tienda",
        bOkAction = "getMarketDetail",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showNoMarketPassedDialog() = showNotificationDialog(
        title = "Error",
        message = "No se encontró la tienda con el identificador proporcionado",
        bOkAction = "return",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
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
}