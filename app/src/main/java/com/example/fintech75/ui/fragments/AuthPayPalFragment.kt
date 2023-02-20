package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.PayPalOrder
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentAuthPayPalBinding
import com.example.fintech75.presentation.MovementViewModel
import com.example.fintech75.presentation.MovementViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import retrofit2.HttpException
import java.security.PrivateKey

class AuthPayPalFragment : Fragment(R.layout.fragment_auth_pay_pal) {
    private val fragmentName = this::class.java.toString()
    private val args: AuthPayPalFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory(
            StartRepositoryImpl(
                RemoteDataSource(RetrofitClient.webService)
            )
        )
    }
    private val movementViewModel: MovementViewModel by viewModels {
        MovementViewModelFactory(
            MovementRepositoryImpl(
                RemoteDataSource(RetrofitClient.webService)
            )
        )
    }

    private lateinit var binding: FragmentAuthPayPalBinding
    private lateinit var screenLoading: RelativeLayout

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    private val checkoutSdk: PayPalCheckout
        get() = PayPalCheckout

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAuthPayPalBinding.bind(view)
        screenLoading = binding.rlAuthPaypalLoading

        setup()
    }

    private fun setup() {
        binding.tvAuthPaypalMsg.text = getString(R.string.loading)
        screenLoading.visibility = View.VISIBLE
        setStateMovementButtons(false, false)

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        backPressedListener()
        configPayPalSDK()

        if (currentUser.userID == -1) {
            showInvalidCredentialsDialog()
        } else {
            bind()

            configCancelButton()
            configMakeButton()

            setStateMovementButtons(true, true)
            screenLoading.visibility = View.GONE
        }
    }

    // Credentials
    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A",
            email = "N/A"
        )
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }
    // End Credentials


    // UI

    private fun bind() {
        if (args.idMovement == -1) {
            showInvalidCredentialsDialog()
        }

        binding.tvTypeMovement.text = args.typeMovement
        binding.tvMethodMovement.text = args.methodMovement
        binding.tvAmountMovement.text = getAmountStringFromFloat(args.amountMovement)
    }

    private fun getAmountStringFromFloat(amountFloat: Float): String {
        var newAmountString = amountFloat.toString()

        if (newAmountString.contains("""\.\d{1,2}""".toRegex())) {
            if (newAmountString.matches("""^\d+\.\d$""".toRegex())) {
                newAmountString += "0"
            }
        } else {
            newAmountString += ".00"
        }
        newAmountString = "$$newAmountString"

        return newAmountString
    }

    private fun setStateMovementButtons(cancelIsAvailable: Boolean, makeIsAvailable: Boolean) {
        binding.bCancelMovement.isEnabled = cancelIsAvailable
        binding.bMakeMovement.isEnabled = makeIsAvailable


        binding.bCancelMovement.background = if (cancelIsAvailable) {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_close_button, context?.theme)
        } else {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
        }

        binding.bMakeMovement.background = if (makeIsAvailable) {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
        } else {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
        }
    }

    private fun configCancelButton() {
        binding.bCancelMovement.setOnClickListener {
            cancelMovement()
        }
    }

    private fun configMakeButton() {
        binding.bMakeMovement.setOnClickListener {
            setStateMovementButtons(false, false)
            beginPayPalAuth()
        }
    }

    // End UI


    // Functions
    private fun beginPayPalAuth() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.generatePayPalOrder(
                currentUser.token, args.idMovement, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Beginning auth movement process")
                        binding.tvAuthPaypalMsg.text = getString(R.string.connecting_paypal)
                        screenLoading.visibility = View.VISIBLE

                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Get PayPal order")
                        val paypalOrder: PayPalOrder = result.data as PayPalOrder

                        Log.d(fragmentName, "PayPal order: ${paypalOrder.id}")
                        startCheckout(paypalOrder.id)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try auth movement again")

                        setStateMovementButtons(true, true)
                        screenLoading.visibility = View.GONE

                        showTryAgainAuthMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail auth movement")
                        when((result.exception as HttpException).code()) {
                            400 -> {
                                Log.d(fragmentName, "Try auth movement again")
                                setStateMovementButtons(true, true)

                                screenLoading.visibility = View.GONE
                                showTryAgainAuthMovementDialog()
                            }
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")

                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")

                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Movement not found")

                                goToResultScreen(false)
                            }
                            409 -> {
                                Log.d(fragmentName, "No enough founds")

                                goToResultScreen(false)
                            }
                            460 -> {
                                Log.d(fragmentName, "Movement conflict")
                                screenLoading.visibility = View.GONE

                                goToResultScreen(false)
                            }

                            else -> {
                                Log.d(fragmentName, "Generic error")

                                setStateMovementButtons(true, true)
                                screenLoading.visibility = View.GONE

                                showTryAgainAuthMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun captureOrder() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.finishPayPalMovement(
                currentUser.token, args.idMovement, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Finishing auth movement process")
                        binding.tvAuthPaypalMsg.text = getString(R.string.finishing_movement)
                        screenLoading.visibility = View.VISIBLE

                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Captured PayPal order successfully")
                        Toast.makeText(
                            requireContext(),
                            "💰 Order Capture Succeeded 💰",
                            Toast.LENGTH_SHORT
                        ).show()

                        goToResultScreen(true)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try finish movement again")

                        setStateMovementButtons(true, true)
                        screenLoading.visibility = View.GONE

                        showTryAgainAuthMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail finish movement")
                        Toast.makeText(
                            requireContext(),
                            "🔥 Order Capture Failed 🔥",
                            Toast.LENGTH_SHORT
                        ).show()

                        when((result.exception as HttpException).code()) {
                            400 -> {
                                Log.d(fragmentName, "Try finish movement again")
                                setStateMovementButtons(true, true)

                                screenLoading.visibility = View.GONE
                                showTryAgainAuthMovementDialog()
                            }
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")

                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")

                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Movement not found")

                                goToResultScreen(false)
                            }
                            409 -> {
                                Log.d(fragmentName, "No enough founds")

                                goToResultScreen(false)
                            }
                            460 -> {
                                Log.d(fragmentName, "Movement conflict")
                                screenLoading.visibility = View.GONE

                                goToResultScreen(false)
                            }

                            else -> {
                                Log.d(fragmentName, "Generic error")

                                setStateMovementButtons(true, true)
                                screenLoading.visibility = View.GONE

                                showTryAgainAuthMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cancelMovement() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.cancelMovement(
                currentUser.token, args.idMovement, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Beginning cancel credit process")
                        binding.tvAuthPaypalMsg.text = getString(R.string.canceling)
                        screenLoading.visibility = View.VISIBLE

                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Finish cancel credit process")

                        goToResultScreen(false)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try cancel credit again")

                        setStateMovementButtons(true, true)

                        screenLoading.visibility = View.GONE
                        showTryAgainCancelMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail auth movement")

                        when((result.exception as HttpException).code()) {
                            400 -> {
                                Log.d(fragmentName, "Try cancel credit again")
                                setStateMovementButtons(true, true)

                                screenLoading.visibility = View.GONE
                                showTryAgainCancelMovementDialog()
                            }
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")

                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")

                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Client not found")

                                goToResultScreen(false)
                            }
                            else -> {
                                Log.d(fragmentName, "Bad credentials")
                                screenLoading.visibility = View.GONE
                                setStateMovementButtons(false, false)

                                showInvalidCredentialsDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    // End Functions


    // Config PayPal

    private fun configPayPalSDK() {
        checkoutSdk.registerCallbacks(
            onApprove = OnApprove { approval ->
                Log.i(fragmentName, "OnApprove: $approval")
                captureOrder()
            },
            onCancel = OnCancel {
                Log.d(fragmentName, "OnCancel")
                Toast.makeText(
                    requireContext(),
                    "😭 Buyer Cancelled Checkout 😭",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = OnError { errorInfo ->
                Log.d(fragmentName, "ErrorInfo: $errorInfo")
                Toast.makeText(
                    requireContext(),
                    "🚨 An Error Occurred 🚨",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun startCheckout(idOrder: String) {
        checkoutSdk.startCheckout(
            createOrder = CreateOrder { actions ->
                actions.set(idOrder)
            }
        )
    }

    // End Config PayPal



    // Navigation
    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

    private fun goToResultScreen(isSuccess: Boolean) {
        Log.d(fragmentName, "Go to result screen...")
        val action = AuthPayPalFragmentDirections.actionAuthPayPalFragmentToMovementFinishedFragment(
            isSuccessful = isSuccess
        )
        findNavController().navigate(action)
    }

    private fun backPressedListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.d(fragmentName, "Cancel auth of movement")
                cancelMovement()
            }
        })
    }

    private fun catchResultFromDialogs() {

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("action")
            ?.observe(viewLifecycleOwner) { action ->
                when(action) {
                    "finishSession" -> goToLogin()
                    "authMovement" -> Log.d(fragmentName, "Auth Movement")
                    "logout" -> logout()
                    "return" -> goToCredits()
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
                    "closeFragment" -> goToCredits()
                    else -> return@observe
                }
            }
    }
    // End Navigation


    // Extra Functions
    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvAuthPaypalMsg.text = getString(R.string.closing_session)
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
    // End Extra functions


    // Dialog fragments
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

    private fun showTryAgainAuthMovementDialog() = showNotificationDialog(
        title = "Movimiento no creado",
        message = "No se pudo crear el Movimiento. Intentelo de nuevo",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showTryAgainCancelMovementDialog() = showNotificationDialog(
        title = "Movimiento no cancelado",
        message = "No se pudo cancelar el movimiento. Intentelo de nuevo",
        bOkAction = "iKnow",
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
        val action = AuthPayPalFragmentDirections.actionAuthPayPalFragmentToNotificationDialogFragment(
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