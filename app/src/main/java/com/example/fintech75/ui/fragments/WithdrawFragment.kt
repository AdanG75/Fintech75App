package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentWithdrawBinding
import com.example.fintech75.presentation.MovementViewModel
import com.example.fintech75.presentation.MovementViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey

class WithdrawFragment : Fragment(R.layout.fragment_withdraw) {
    private val fragmentName = this::class.java.toString()
    private val args: WithdrawFragmentArgs by navArgs()
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

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    private lateinit var binding: FragmentWithdrawBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var withdrawButton: Button

    private var originCredit: Int = -1
    private var amount: Double = -1.0

    private var validOriginCredit: Boolean = false
    private var validAmount: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentWithdrawBinding.bind(view)
        screenLoading = binding.rlWithdrawLoading
        withdrawButton = binding.bWithdraw

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        setStateWithdrawButton(false)

        if (currentUser.userID == -1 || currentUser.typeUser != "market") {
            showInvalidCredentialsDialog()
        } else {
            configCreditView()
            withdrawButtonStatusListener()

            addAmountEditTextListener()
            addCreditEditTextListener()

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

    private fun configCreditView() {
        if (args.originCreditId == -1) {
            binding.etOriginCredit.visibility = View.VISIBLE
            binding.tvOriginCredit.visibility = View.GONE
        } else {
            originCredit = args.originCreditId
            validOriginCredit = true
            binding.tvOriginCredit.text = args.originCreditId.toString()

            binding.etOriginCredit.visibility = View.GONE
            binding.tvOriginCredit.visibility = View.VISIBLE
        }

        withdrawDataChange()
    }

    private fun withdraw() {
        Log.d(fragmentName, "Withdraw function")
        userPrivateKey?.let { privateKey ->
            movementViewModel.generateWithdrawSummary(
                currentUser.token, originCredit, amount, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating movement's summary...")
                        screenLoading.visibility = View.VISIBLE
                        setStateWithdrawButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting movement's summary")
                        val movementSummary: MovementExtraRequest = result.data as MovementExtraRequest

                        goToMovementSummary(movementSummary)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when generating movement's summary")
                        screenLoading.visibility = View.GONE
                        setStateWithdrawButton(true)
                        showTryAgainWithdrawDialog()
                    }
                    is Resource.Failure -> {
                        when((result.exception as HttpException).code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                showInvalidCredentialsDialog()
                            }
                            404 -> {
                                Log.d(fragmentName, "Credit not found")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetWithdrawValues()
                                withdrawDataChange()

                                showNoCreditFoundDialog()
                            }
                            409 -> {
                                Log.d(fragmentName, "Not enough funds")
                                screenLoading.visibility = View.GONE
                                setStateWithdrawButton(true)
                                showNotEnoughFundsDialog()
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetWithdrawValues()
                                withdrawDataChange()

                                showUnauthorizedCreditDialog()
                            }
                            else -> {
                                Log.d(fragmentName, "Bad credentials")
                                showInvalidCredentialsDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cleanEditTexts() {
        binding.etOriginCredit.text.clear()
        binding.etAmount.text.clear()
    }

    private fun resetWithdrawValues() {
        originCredit = -1
        validOriginCredit = false

        amount = -1.0
        validAmount = false

        if (args.originCreditId != -1) {
            originCredit = args.originCreditId
            validOriginCredit = true
        }
    }

    private fun setStateWithdrawButton(isEnable: Boolean) {
        withdrawButton.isEnabled = isEnable

        if (isEnable) {
            withdrawButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            withdrawButton.setOnClickListener {
                closeKeyboard()
                withdraw()
            }

        } else {
            withdrawButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            withdrawButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun withdrawButtonStatusListener() {
        movementViewModel.getValidWithdrawForm().observe(viewLifecycleOwner) { result: Boolean ->
            setStateWithdrawButton(result)
        }
    }

    private fun withdrawDataChange() {
        movementViewModel.validateWithdrawForm(validOriginCredit, validAmount)
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

    private fun goToMovementSummary(movementSummary: MovementExtraRequest) {
        Log.d(fragmentName, "Go to summary movement...")

        cleanEditTexts()
        resetWithdrawValues()
        withdrawDataChange()
        val action = WithdrawFragmentDirections.actionWithdrawFragmentToMovementSummaryFragment(
            movementSummary = movementSummary
        )
        findNavController().navigate(action)
    }

    private fun closeKeyboard() {
        GlobalSettings.inputMethodManager?.let {
            it.hideSoftInputFromWindow(binding.root.windowToken, 0)
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
                    binding.tvWithdrawMsg.text = getString(R.string.closing_session)
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

    private fun addAmountEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                var auxAmount: Double = 0.0

                if (binding.etAmount.text.toString().matches(
                        """^\d+(\.\d{1,2})?$""".toRegex()
                    )) {
                    auxAmount = binding.etAmount.text.toString().toDouble()
                    validAmount = auxAmount > 0

                    binding.etAmount.error = if (!validAmount) {
                        "La cantidad debe ser mayor a cero"
                    } else {
                        null
                    }

                } else {
                    validAmount= false
                    binding.etAmount.error = "Cifra no valida"
                }

                amount = if (validAmount) {
                    auxAmount
                } else {
                    -1.0
                }
                withdrawDataChange()
            }
        }

        binding.etAmount.addTextChangedListener(afterTextChangedListener)
    }

    private fun addCreditEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                if (binding.tvOriginCredit.visibility == View.GONE) {
                    val resultET = checkCreditFormat(binding.etOriginCredit)
                    originCredit = resultET.second
                    validOriginCredit = resultET.first
                }

                withdrawDataChange()
            }
        }

        binding.etOriginCredit.addTextChangedListener(afterTextChangedListener)
    }

    private fun checkCreditFormat(currentET: EditText): Pair<Boolean, Int> {
        return if (currentET.text.toString().matches(
                """^\d+$""".toRegex()
            )) {
            val auxCredit = currentET.text.toString().toInt()
            val validCredit = auxCredit > 0

            currentET.error = if (!validCredit) {
                "El crédito debe ser mayor a cero"
            } else {
                null
            }

            Pair(validCredit, auxCredit)

        } else {
            currentET.error= "Formato de crédito no valido"
            Pair(false, -1)
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
                    "withdraw" -> withdraw()
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

    private fun showTryAgainWithdrawDialog() = showNotificationDialog(
        title = "Error al transferir",
        message = "Un error inesperado ocurrió. Favor de intertarlo de nuevo.",
        bOkAction = "withdraw",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showUnauthorizedCreditDialog() = showNotificationDialog(
        title = "Error",
        message = "Usted no tiene permiso para usar este crédito",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showNotEnoughFundsDialog() = showNotificationDialog(
        title = "Error al transferir",
        message = "Fondos insuficientes.",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showNoCreditFoundDialog() = showNotificationDialog(
        title = "Error",
        message = "Crédito no encontrado",
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
        val action = WithdrawFragmentDirections.actionWithdrawFragmentToNotificationDialogFragment(
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