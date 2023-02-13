package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
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
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val movementViewModel: MovementViewModel by viewModels {
        MovementViewModelFactory( MovementRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentMovementSummaryBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var makeButton: Button
    private lateinit var cancelButton: Button

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMovementSummaryBinding.bind(view)
        screenLoading = binding.rlSummaryLoading
        makeButton = binding.bMakeMovement
        cancelButton = binding.bCancelMovement

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
            binding.tvDestinationCreditMovement.text = movementSummary.extra.destinationCredit.toString()
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
        return when(typeMovement.lowercase()) {
            "payment" -> "Pago"
            "deposit" -> "Depósito"
            "transfer" -> "Transferencia"
            "withdraw" -> "Retiro"
            else -> "Transacción"
        }
    }

    private fun getMethodMovement(method: String): String {
        return when(method.lowercase()) {
            "paypal" -> "PayPal"
            "cash" -> "Efectivo"
            else -> "Crédito"
        }
    }

    private fun beginMovement() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.beginMovement(
                currentUser.token, args.movementSummary, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating movement...")
                        screenLoading.visibility = View.VISIBLE
                        binding.llButtons.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Movement has been created")
                        val movement: MovementComplete = result.data as MovementComplete

                        Log.d(fragmentName, "Type sub movement: ${movement.extra.typeSubMovement}")
                        // goToAuth(movement)

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
                        if ((result.exception as HttpException).code() == 409) {
                            Log.d(fragmentName, "Not enough funds")
                            screenLoading.visibility = View.GONE
                            binding.llButtons.visibility = View.VISIBLE

                            showNotEnoughFundsDialog()
                        } else {
                            Log.d(fragmentName, "Bad credentials")
                            showInvalidCredentialsDialog()
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

    private fun showNotEnoughFundsDialog() = showNotificationDialog(
        title = "Error al iniciar movimiento",
        message = "Fondos insuficientes.",
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