package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentMovementSummaryBinding
import com.example.fintech75.presentation.MovementViewModel
import com.example.fintech75.presentation.MovementViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey

class MovementSummaryFragment : Fragment(R.layout.fragment_movement_summary) {
    private val fragmentName = this::class.java.toString()
    private val args: MovementSummaryFragmentArgs by navArgs()
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

    private lateinit var binding: FragmentMovementSummaryBinding
    private lateinit var screenLoading: RelativeLayout

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null
    private var currentMovement: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMovementSummaryBinding.bind(view)
        screenLoading = binding.rlSummaryLoading

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE
        binding.llButtons.visibility = View.GONE

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        if (currentUser.userID == -1) {
            showInvalidCredentialsDialog()
        } else {
            bind(args.movementSummary)

            binding.bMakeMovement.setOnClickListener {
                beginMovement()
            }

            binding.bCancelMovement.setOnClickListener {
                goToCredits()
            }

            binding.llButtons.visibility = View.VISIBLE
            screenLoading.visibility = View.GONE
        }

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

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun bind(movementSummary: MovementExtraRequest) {
        binding.tvTypeMovement.text = getTypeMovement(movementSummary.typeMovement)
        binding.tvMethodMovement.text = getMethodMovement(movementSummary.extra.typeSubMovement)

        "$${movementSummary.amount}".also {
            binding.tvAmountMovement.text = it
        }

        if (movementSummary.idCredit == null) {
            binding.tvOriginCreditMovementText.visibility = View.GONE
            binding.tvOriginCreditMovement.visibility = View.GONE
        } else {
            binding.tvOriginCreditMovement.text = movementSummary.idCredit.toString()
        }

        if (movementSummary.extra.destinationCredit == null) {
            binding.tvDestinationCreditMovementText.visibility = View.GONE
            binding.tvDestinationCreditMovement.visibility = View.GONE
        } else {
            binding.tvDestinationCreditMovement.text =
                movementSummary.extra.destinationCredit.toString()
        }

        if (movementSummary.extra.idMarket == null) {
            binding.tvMarketMovementText.visibility = View.GONE
            binding.tvMarketMovement.visibility = View.GONE
        } else {
            binding.tvMarketMovement.text = movementSummary.extra.idMarket
        }

        if (movementSummary.extra.depositorName == null) {
            binding.tvDepositorNameMovementText.visibility = View.GONE
            binding.tvDepositorNameMovement.visibility = View.GONE
        } else {
            binding.tvDepositorNameMovement.text = movementSummary.extra.depositorName
        }
    }

    private fun getTypeMovement(typeMovement: String): String {
        return when (typeMovement.lowercase()) {
            "payment" -> "Pago"
            "deposit" -> "Depósito"
            "transfer" -> "Transferencia"
            "withdraw" -> "Retiro"
            else -> "Transacción"
        }
    }

    private fun getMethodMovement(method: String): String {
        return when (method.lowercase()) {
            "paypal" -> "PayPal"
            "cash" -> "Efectivo"
            else -> "Crédito"
        }
    }

    private fun getAmountFloatFromMovementComplete(amountString: String): Float {
        val cleanAmountString = amountString
            .replace("$", "")
            .replace(",", "")
            .replace("""\s""".toRegex(), "")

        return cleanAmountString.toFloat()
    }

    private fun beginMovement() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.beginMovement(
                currentUser.token, args.movementSummary, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating movement...")
                        screenLoading.visibility = View.VISIBLE
                        binding.llButtons.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Movement has been created")
                        val movement: MovementComplete = result.data as MovementComplete

                        Log.d(fragmentName, "ID movement: ${movement.idMovement} Type sub movement: ${movement.extra.typeSubMovement}")
                        currentMovement = movement.idMovement
                        goToAuth(movement)

                        screenLoading.visibility = View.GONE
                        binding.llButtons.visibility = View.VISIBLE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when generating movement")
                        screenLoading.visibility = View.GONE
                        binding.llButtons.visibility = View.VISIBLE

                        showTryAgainBeginMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail to begin movement")
                        val exception = (result.exception as HttpException)
                        when (exception.code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                showInvalidCredentialsDialog()
                            }
                            403, 404, 409, 450, 451, 460 -> {
                                Log.d(fragmentName, "Error message: ${exception.message()}")
                                goToScreenResult(false)
                            }
                            else -> {
                                Log.d(fragmentName, "An error occurs when generating movement")
                                screenLoading.visibility = View.GONE
                                binding.llButtons.visibility = View.VISIBLE

                                showTryAgainBeginMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToAuth(movement: MovementComplete) {
        val authType = when (movement.typeMovement.lowercase()) {
            AppConstants.DEPOSIT_MOVEMENT -> {
                if (movement.extra.typeSubMovement.lowercase() == "paypal") {
                    AppConstants.AUTH_PAYPAL
                } else {
                    AppConstants.AUTH_NONE
                }
            }
            AppConstants.PAY_MOVEMENT -> {
                if (movement.extra.typeSubMovement.lowercase() == "paypal") {
                    AppConstants.AUTH_PAYPAL
                } else {
                    AppConstants.AUTH_FINGERPRINT
                }
            }
            AppConstants.TRANSFER_MOVEMENT -> {
                if (movement.extra.typeSubMovement == "localG") {
                    AppConstants.AUTH_BOTH
                } else {
                    AppConstants.AUTH_FINGERPRINT
                }
            }
            AppConstants.WITHDRAW_MOVEMENT -> {
                AppConstants.AUTH_FINGERPRINT
            }
            else -> {
                AppConstants.AUTH_FINGERPRINT
            }
        }
        when(authType) {
            AppConstants.AUTH_FINGERPRINT, AppConstants.AUTH_BOTH -> {
                goToAuthFingerprint(
                    idMovement = movement.idMovement,
                    typeMovement = getTypeMovement(movement.typeMovement),
                    amountMovement = getAmountFloatFromMovementComplete(movement.amount),
                    methodMovement = getMethodMovement(movement.extra.typeSubMovement),
                    authType = authType
                )
            }
            AppConstants.AUTH_PAYPAL -> {
                goToAuthPayPal(
                    idMovement = movement.idMovement,
                    typeMovement = getTypeMovement(movement.typeMovement),
                    amountMovement = getAmountFloatFromMovementComplete(movement.amount),
                    methodMovement = getMethodMovement(movement.extra.typeSubMovement),
                    authType = authType
                )
            }
            AppConstants.AUTH_NONE -> {
                executeMovement()
                goToScreenResult(true)
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Tipo de autorización no valido",
                    Toast.LENGTH_SHORT
                ).show()
                goToCredits()
            }

        }
    }

    private fun executeMovement() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.executeMovement(
                currentUser.token, currentMovement, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Executing movement...")
                        screenLoading.visibility = View.VISIBLE
                        binding.tvSummaryMsg.text = getString(R.string.executing_movement)
                        binding.llButtons.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Movement has been executed")
                        val movement: MovementComplete = result.data as MovementComplete

                        Log.d(fragmentName, "ID movement: ${movement.idMovement} Type sub movement: ${movement.extra.typeSubMovement}")

                        goToScreenResult(true)

                        screenLoading.visibility = View.GONE
                        binding.llButtons.visibility = View.VISIBLE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when executing movement")
                        screenLoading.visibility = View.GONE
                        binding.llButtons.visibility = View.VISIBLE

                        showTryAgainExecuteMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail to execute movement")
                        val exception = (result.exception as HttpException)
                        when (exception.code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                showInvalidCredentialsDialog()
                            }
                            403, 404, 409, 450, 451, 460 -> {
                                Log.d(fragmentName, "Error message: ${exception.message()}")
                                goToScreenResult(false)
                            }
                            else -> {
                                Log.d(fragmentName, "An error occurs when executing movement")
                                screenLoading.visibility = View.GONE
                                binding.llButtons.visibility = View.VISIBLE

                                showTryAgainExecuteMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

    private fun goToAuthFingerprint(
        idMovement: Int,
        typeMovement: String,
        amountMovement: Float,
        methodMovement: String,
        authType: String
    ) {
        Log.d(fragmentName, "Go to Auth Fingerprint...")
        val action =
            MovementSummaryFragmentDirections.actionMovementSummaryFragmentToAuthFingerprintFragment(
                idMovement = idMovement,
                typeMovement = typeMovement,
                amountMovement = amountMovement,
                methodMovement = methodMovement,
                authType = authType
            )
        findNavController().navigate(action)
    }

    private fun goToAuthPayPal(
        idMovement: Int,
        typeMovement: String,
        amountMovement: Float,
        methodMovement: String,
        authType: String
    ) {
        Log.d(fragmentName, "Go to Auth PayPal...")
        val action =
            MovementSummaryFragmentDirections.actionMovementSummaryFragmentToAuthPayPalFragment(
                idMovement = idMovement,
                typeMovement = typeMovement,
                amountMovement = amountMovement,
                methodMovement = methodMovement,
                authType = authType
            )
        findNavController().navigate(action)
    }

    private fun goToScreenResult(isSuccessful: Boolean) {
        Log.d(fragmentName, "Go to Result...")
        val action =
            MovementSummaryFragmentDirections.actionMovementSummaryFragmentToMovementFinishedFragment(
                isSuccessful = isSuccessful
            )
        findNavController().navigate(action)
    }

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvSummaryMsg.text = getString(R.string.closing_session)
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
                    "begin" -> beginMovement()
                    "execute" -> executeMovement()
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

    private fun showTryAgainBeginMovementDialog() = showNotificationDialog(
        title = "Error al iniciar movimiento",
        message = "Un error inesperado ocurrió. Favor de intertarlo de nuevo.",
        bOkAction = "begin",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showTryAgainExecuteMovementDialog() = showNotificationDialog(
        title = "Error al ejecutar movimiento",
        message = "Un error inesperado ocurrió. Favor de intertarlo de nuevo.",
        bOkAction = "execute",
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
        val action = MovementSummaryFragmentDirections.actionMovementSummaryFragmentToNotificationDialogFragment(
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