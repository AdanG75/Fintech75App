package com.example.fintech75.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.application.ItemClickListener
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.CreditBase
import com.example.fintech75.data.model.CreditList
import com.example.fintech75.data.model.MarketFull
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentCreditsBinding
import com.example.fintech75.presentation.StartViewModel
import com.example.fintech75.presentation.StartViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.example.fintech75.ui.adapters.CreditAdapter
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.properties.Delegates

class CreditsFragment : Fragment(R.layout.fragment_credits), ItemClickListener {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val startViewModel: StartViewModel by activityViewModels {
        StartViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentCreditsBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var buttonSettings: ImageButton
    private lateinit var buttonPayments: Button
    private lateinit var refreshItem: SwipeRefreshLayout
    private lateinit var withoutCreditsCard: CardView
    private lateinit var rvCredits: RecyclerView
    private var fragmentDestinationID by Delegates.notNull<Int>()

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null
    private var userPublicKey: PublicKey? = null
    private var serverPublicKey: PublicKey? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCreditsBinding.bind(view)
        screenLoading = binding.rlCreditsLoading
        buttonSettings = binding.bSettings
        buttonPayments = binding.bPayments
        refreshItem = binding.srlRefresh
        withoutCreditsCard = binding.cvWithoutCredits
        rvCredits = binding.rvCredits
        fragmentDestinationID = findNavController().currentDestination?.id ?: 0

        creditsFragmentSetup()
    }

    private fun creditsFragmentSetup() {
        currentUserListener()

        (activity as MainActivity).hideBottomNavigation()
        setStatePaymentButton(false)
        setStateSettingsButton(false)

        getStatusStartSetup()
        catchResultFromDialogs()
        backPressedListener()
        refreshListener()

        userPublicKeyListener()
        userPrivateKeyListener()
        serverPublicKeyListener()
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A"
        )
        checkUserIsLogged(currentUser)
    }

    private fun checkUserIsLogged(user: UserCredential) {
        if (user.typeUser == "N/A"){
            Log.d(fragmentName, "No user logged")
            goToLogin()
        } else {
            Log.d(fragmentName, "Beginning ${user.typeUser}'s session.")
        }
    }

    private fun goToLogin() {
        screenLoading.visibility = View.GONE
        Log.d(fragmentName, "Go to Login...")
        val currentDestination = findNavController().currentDestination?.id ?: -1
        // Log.d(fragmentName, "Current destination: $currentDestination, credits fragment destination: $fragmentDestinationID")

        if (fragmentDestinationID == 0 || currentDestination == -1) {
            findNavController().navigate(R.id.loginFragment)
        } else if (currentUser.userID == -1) {
            Log.d(fragmentName, "Without logged user")
            val action = CreditsFragmentDirections.actionCreditsFragmentToLoginFragment()
            findNavController().navigate(action)
        } else {
            Log.d(fragmentName, "With logged user")
            findNavController().popBackStack(R.id.loginFragment, false)
        }
    }

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    setStateSettingsButton(false)
                    setStatePaymentButton(false)
                    screenLoading.visibility = View.VISIBLE
                    binding.tvCreditsMsg.text = getString(R.string.closing_session)
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Good bye")
                    goToLogin()
                }
                is Resource.TryAgain -> {
                    screenLoading.visibility = View.GONE
                    setStateSettingsButton(true)
                    setStatePaymentButton(true)
                    Log.d(fragmentName, "Un error ocurrió, favor de intentarlo de nuevo")
                    showNotificationDialog(
                        title = "Error",
                        message = "No se pudo cerrar la sesión",
                        bOkAction = "logout",
                        bOkText = getString(R.string.try_again),
                        bOkAvailable = true,
                        bCancelAction = AppConstants.ACTION_CLOSE_APP,
                        bCancelText = getString(R.string.close_app),
                        bCancelAvailable = true
                    )
                }
                is Resource.Failure -> goToLogin()
            }
        }
    }

    private fun userPrivateKeyListener() {
        userViewModel.getUserPrivateKey().observe(viewLifecycleOwner) { privateKey ->
            userPrivateKey = privateKey
        }
    }

    private fun userPublicKeyListener() {
        userViewModel.getUserPublicKey().observe(viewLifecycleOwner) { publicKey ->
            userPublicKey = publicKey
        }
    }

    private fun serverPublicKeyListener() {
        startViewModel.getPublicServerKey().observe(viewLifecycleOwner) { sPublicKey ->
            serverPublicKey = sPublicKey
        }
    }

    private fun getStatusStartSetup() {
        startViewModel.getStartSetupState().observe(viewLifecycleOwner) { state ->
            Log.d(fragmentName, "Status of system's setup $state")
            when(state) {
                AppConstants.MESSAGE_STATE_NONE -> startSetup()
                AppConstants.MESSAGE_STATE_SUCCESS -> {
                    Log.d(fragmentName, "Start has finished")
                    getStatusUserSetup()
                }
                AppConstants.MESSAGE_STATE_TRY_AGAIN -> {
                    Log.d(fragmentName, "Try to charge the server's setup again")
                    showTryAgainFetchServerKeyDialog()
                }
                AppConstants.MESSAGE_STATE_FAILURE -> {
                    Log.d(fragmentName, "Couldn't fetch server's public key")
                    showTryAgainFetchServerKeyDialog()
                }
                AppConstants.MESSAGE_STATE_LOADING -> Log.d(fragmentName, "Loading system's credentials")
                else -> Log.d(fragmentName, "Unexpected state")
            }
        }
    }

    private fun startSetup() {
        startViewModel.setupStart().observe(viewLifecycleOwner) { result: Resource<*> ->
            when(result) {
                is Resource.Loading -> {
                    Log.d(fragmentName, "Loading start setup...")
                    screenLoading.visibility = View.VISIBLE
                }
                is Resource.Success -> Log.d(fragmentName, "Start setup has finished")
                is Resource.TryAgain -> {
                    Log.w(fragmentName, "An unexpected error occurs. Try to fetch the server's public keys again.")
                    showTryAgainFetchServerKeyDialog()
                }
                is Resource.Failure -> Log.e(fragmentName, "An error occurs when fetching the server's public keys")
            }
        }
    }

    private fun getStatusUserSetup() {
        userViewModel.getUserSetupStatus().observe(viewLifecycleOwner) { status ->
            Log.d(fragmentName, "Status of user's setup $status")
            when(status) {
                AppConstants.MESSAGE_STATE_NONE -> userSetup()
                AppConstants.MESSAGE_STATE_SUCCESS -> {
                    Log.d(fragmentName, "User's setup has finished")
                    (activity as MainActivity).showBottomNavigation()
                    getCreditsUser()
                }
                AppConstants.MESSAGE_STATE_TRY_AGAIN -> {
                    Log.d(fragmentName, "Try to load the user's setup again")
                    showTryAgainUserSetupDialog()
                }
                AppConstants.MESSAGE_STATE_FAILURE -> {
                    Log.d(fragmentName, "Couldn't set up the user's credentials")
                    showTryAgainUserSetupDialog()
                }
                AppConstants.MESSAGE_STATE_LOADING -> {
                    Log.d(fragmentName, "Loading user's credentials")
                    screenLoading.visibility = View.VISIBLE
                }
                AppConstants.MESSAGE_STATE_FATAL_FAILURE -> {
                    Log.d(fragmentName, "Bad credentials")
                    showInvalidCredentialsDialog()
                }
                else -> Log.d(fragmentName, "Unexpected state")
            }
        }
    }

    private fun userSetup() {
        userViewModel
            .userSetup(currentUser.token, currentUser.userID, currentUser.typeUser)
            .observe(viewLifecycleOwner) {result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading user's setup...")
                        screenLoading.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "User's setup has finished")
                        if (result.data is Boolean) {
                            if (!result.data) {
                                screenLoading.visibility = View.GONE
                                goToRegisterFingerprint()
                            }
                        }
                    }
                    is Resource.TryAgain -> Log.w(fragmentName, "An unexpected error occurs. Try to set up the user's credentials again.")
                    is Resource.Failure -> Log.e(fragmentName, "An error occurs when set up the user's credentials")
                }
            }
    }

    private fun getCreditsUser() {
        // val pkMessage: String = userPrivateKey?.toString() ?: "null"
        // Log.d(fragmentName, "Private key: $pkMessage")
        userPrivateKey?.let { privateKey ->
            userViewModel
                .fetchCreditsUser(currentUser.token, currentUser.userID, privateKey)
                .observe(viewLifecycleOwner) { result: Resource<*> ->
                    Log.d(fragmentName, "Credit status: ${result.javaClass.name}")
                    when(result) {
                        is Resource.Loading -> {
                            screenLoading.visibility = View.VISIBLE
                            Log.d(fragmentName, "Loading user's credits...")
                        }
                        is Resource.Success -> {
                            Log.d(fragmentName, "Successfully obtaining user's credits")
                            val creditsUser: CreditList = result.data as CreditList
                            rvCredits.adapter = CreditAdapter(creditsUser.credits, this)

                            if (creditsUser.credits.isEmpty()) {
                                rvCredits.visibility = View.GONE
                                withoutCreditsCard.visibility = View.VISIBLE
                            } else {
                                extractUserGlobalCredit(creditsUser.credits)
                                rvCredits.visibility = View.VISIBLE
                                withoutCreditsCard.visibility = View.GONE
                            }
                            refreshItem.isRefreshing = false
                            screenLoading.visibility = View.GONE
                            setStatePaymentButton(true)
                            setStateSettingsButton(true)
                        }
                        is Resource.TryAgain -> {
                            Log.d(fragmentName, "An error occurs when fetching the user's credits. Please, try again")
                            showTryAgainFetchUserCreditsDialog()
                        }
                        is Resource.Failure -> {
                            Log.d(fragmentName, "Bad credentials")
                            showInvalidCredentialsDialog()
                        }
                    }
                }
        }

    }

    private fun extractUserGlobalCredit(credits: List<CreditBase>) {
        userViewModel.extractGlobalCreditId(credits, currentUser.typeUser).observe(viewLifecycleOwner) { result ->
            when(result) {
                is Resource.Loading -> Log.d(fragmentName, "Extracting user's global credit...")
                is Resource.Success -> {
                    val globalCredit : Int? = userViewModel.getUserGlobalCredit().value
                    Log.d(fragmentName, "Finish user's global credit extraction with ID: $globalCredit")
                }
                else -> Log.d(fragmentName, "An error occurs whe extracting user's global credit")
            }
        }
    }

    private fun goToRegisterFingerprint() {
        Log.d(fragmentName, "Go to register fingerprint fragment")
        val action = CreditsFragmentDirections.actionCreditsFragmentToFingerprintRegisterFragment(idClient = currentUser.idType)
        findNavController().navigate(action)
    }

    private fun setStatePaymentButton(isEnable: Boolean) {
        buttonPayments.isEnabled = isEnable

        if (isEnable) {
            buttonPayments.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_secondary_button, context?.theme)
            buttonPayments.setOnClickListener {
                val action = CreditsFragmentDirections.actionCreditsFragmentToPaymentsFragment(currentUser.userID)
                findNavController().navigate(action)
            }
        } else {
            buttonPayments.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            buttonPayments.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun setStateSettingsButton(isEnable: Boolean) {
        if (isEnable) {
            buttonSettings.setOnClickListener {
                val action = CreditsFragmentDirections
                    .actionCreditsFragmentToSettingsFragment(currentUser.typeUser, currentUser.token)
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
        refreshItem.setOnRefreshListener {
            getCreditsUser()
        }
    }

    private fun catchResultFromDialogs() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("result")
            ?.observe(viewLifecycleOwner) { result ->
                when (result) {
                    true -> logout()
                    false -> return@observe
                }
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("action")
            ?.observe(viewLifecycleOwner) { action ->
                when(action) {
                    "logout" -> logout()
                    "startSetup" -> startSetup()
                    "userSetup" -> userSetup()
                    "finishSession" -> goToLogin()
                    "userCredits" -> getCreditsUser()
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

    private fun backPressedListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.d(fragmentName, "Logout...")
                when(currentUser.typeUser) {
                    "N/A" -> goToLogin()
                    else -> {
                        val action = CreditsFragmentDirections.actionCreditsFragmentToLogoutDialogFragment()
                        findNavController().navigate(action)
                    }
                }

            }
        })
    }

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean, closeAction: String = "none"
    ) {
        val action = CreditsFragmentDirections.actionCreditsFragmentToNotificationDialogFragment(
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

    private fun showTryAgainFetchServerKeyDialog() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cargar las credenciales del sistema",
        bOkAction = "startSetup",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    private fun showTryAgainUserSetupDialog() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cargar las credenciales del usuario",
        bOkAction = "userSetup",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showTryAgainFetchUserCreditsDialog() = showNotificationDialog(
        title = "Créditos no cargados",
        message = "No se pudo cargar los créditos del usuario",
        bOkAction = "userCredits",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

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

    override fun onCreditClick(credit: CreditBase) {
        Log.d(fragmentName, "Go to credit details")
        val action = CreditsFragmentDirections.actionCreditsFragmentToCreditDetailFragment(credit)
        findNavController().navigate(action)
    }

    override fun onMarketClick(market: MarketFull) {
        return Unit
    }
}