package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentDepositBinding
import com.example.fintech75.presentation.MovementViewModel
import com.example.fintech75.presentation.MovementViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey

class DepositFragment : Fragment(R.layout.fragment_deposit) {
    private val fragmentName = this::class.java.toString()
    private val args: DepositFragmentArgs by navArgs()
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

    private var depositMethod: String = AppConstants.CREDIT_METHOD
    private var depositCredit: Int = -1
    private var depositorName: String = "N/A"
    private var depositorEmail: String = "N/A"
    private var depositAmount: Double = -1.0

    private var validCredit: Boolean = false
    private var validDepositorName: Boolean = false
    private var validDepositorEmail: Boolean = false
    private var validAmount: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    private lateinit var binding: FragmentDepositBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var depositButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDepositBinding.bind(view)
        screenLoading = binding.rlDepositLoading
        depositButton = binding.bDeposit

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        setStateDepositButton(false)

        if (currentUser.userID == -1) {
            showInvalidCredentialsDialog()
        } else {
            setDepositMethod()
            configCreditView()
            configDepositorView()

            if (currentUser.typeUser == "client") {
                binding.cvRequiredPaypal.visibility = View.VISIBLE
            } else {
                binding.cvRequiredPaypal.visibility = View.GONE
            }

            depositButtonStatusListener()
            addCreditEditTextListener()
            addDepositorNameEditTextListener()
            addDepositorEmailEditTextListener()
            addAmountEditTextListener()

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

    private fun setDepositMethod() {
        depositMethod = when(currentUser.typeUser) {
            "client" ->  AppConstants.PAYPAL_METHOD
            "market" -> AppConstants.CASH_METHOD
            else -> AppConstants.CREDIT_METHOD
        }
    }

    private fun setStateDepositButton(isEnable: Boolean) {
        depositButton.isEnabled = isEnable

        if (isEnable) {
            depositButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            depositButton.setOnClickListener {
                closeKeyboard()
                deposit()
            }

        } else {
            depositButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            depositButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun depositButtonStatusListener() {
        movementViewModel.getValidDepositForm().observe(viewLifecycleOwner) { result: Boolean ->
            setStateDepositButton(result)
        }
    }

    private fun configCreditView() {
        if (args.destinationCreditId == -1) {
            binding.etDestinationCredit.visibility = View.VISIBLE
            binding.tvDestinationCredit.visibility = View.GONE
        } else {
            depositCredit = args.destinationCreditId
            validCredit = true
            binding.tvDestinationCredit.text = args.destinationCreditId.toString()

            binding.etDestinationCredit.visibility = View.GONE
            binding.tvDestinationCredit.visibility = View.VISIBLE
        }

        depositDataChange()
    }

    private fun configDepositorView() {
        if (currentUser.typeUser == "client" && currentUser.email != "N/A") {
            binding.cvDepositor.visibility = View.GONE

            depositorEmail = currentUser.email
            validDepositorEmail = true

            depositorName = getString(R.string.name)
            validDepositorName = true
        } else {
            validDepositorEmail = false
            validDepositorName = false

            binding.cvDepositor.visibility = View.VISIBLE
        }
        depositDataChange()
    }

    private fun deposit() {
        Log.d(fragmentName, "Deposit function")
        // Log.d(fragmentName, "Current email: ${currentUser.email}")
        userPrivateKey?.let { privateKey ->
            movementViewModel.generateDepositSummary(
                currentUser.token, depositMethod, depositCredit, depositorName, depositorEmail, depositAmount, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating movement's summary...")
                        screenLoading.visibility = View.VISIBLE
                        setStateDepositButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting movement's summary")
                        val movementSummary: MovementExtraRequest = result.data as MovementExtraRequest

                        goToMovementSummary(movementSummary)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when generating movement's summary")
                        screenLoading.visibility = View.GONE
                        setStateDepositButton(true)
                        showTryAgainDepositDialog()
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
                                resetDepositValues()
                                depositDataChange()

                                showNoCreditFoundDialog()
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetDepositValues()
                                depositDataChange()

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

    private fun depositDataChange() {
        movementViewModel.validateDepositForm(validCredit, validDepositorName, validDepositorEmail, validAmount)
    }

    private fun cleanEditTexts() {
        binding.etDestinationCredit.text.clear()
        binding.etDepositorName.text.clear()
        binding.etDepositorEmail.text.clear()
        binding.etAmount.text.clear()
    }

    private fun resetDepositValues() {
        depositCredit = -1
        validCredit = false

        depositorName = "N/A"
        validDepositorName = false

        depositorEmail = "N/A"
        validDepositorEmail = false

        depositAmount = -1.0
        validAmount = false

        if (args.destinationCreditId != -1) {
            depositCredit = args.destinationCreditId
            validCredit = true
        }

        if (currentUser.typeUser == "client" && currentUser.email != "N/A") {
            depositorEmail = currentUser.email
            validDepositorEmail = true

            depositorName = getString(R.string.name)
            validDepositorName = true
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

    private fun goToMovementSummary(movementSummary: MovementExtraRequest) {
        Log.d(fragmentName, "Go to summary movement...")

        cleanEditTexts()
        resetDepositValues()
        depositDataChange()
        val action = DepositFragmentDirections.actionDepositFragmentToMovementSummaryFragment(
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
                    binding.tvDepositMsg.text = getString(R.string.closing_session)
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

    private fun addCreditEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                if (binding.etDestinationCredit.text.toString().matches(
                        """^\d+$""".toRegex()
                    )) {
                    depositCredit = binding.etDestinationCredit.text.toString().toInt()
                    validCredit = depositCredit > 0

                    binding.etDestinationCredit.error = if (!validCredit) {
                        depositCredit = -1
                        "El crédito debe ser mayor a cero"
                    } else {
                        null
                    }

                } else {
                    depositCredit = -1
                    validCredit = false
                    binding.etDestinationCredit.error = "Formato de crédito no valido"
                }
                depositDataChange()
            }
        }

        binding.etDestinationCredit.addTextChangedListener(afterTextChangedListener)
    }

    private fun addDepositorNameEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                if (binding.etDepositorName.text.toString().matches(
                        """^[A-Z][A-Za-z áéíóúüöÁÉÍÓÚÖÜ\-]+$""".toRegex()
                    )) {
                    depositorName = binding.etDepositorName.text.toString().trim()
                    validDepositorName = depositorName.length > 2

                    binding.etDepositorName.error = if (!validDepositorName) {
                        depositorName = "N/A"
                        "El nombre debe tener más de 2 caracteres"
                    } else {
                        null
                    }

                } else {
                    depositorName = "N/A"
                    validDepositorName = false
                    binding.etDepositorName.error = "Nombre no valido"
                }
                depositDataChange()
            }
        }

        binding.etDepositorName.addTextChangedListener(afterTextChangedListener)
    }

    private fun addDepositorEmailEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            //NO CREDITS
            override fun afterTextChanged(s: Editable) {
                val auxEmail = binding.etDepositorEmail.text.toString()
                validDepositorEmail =  if (auxEmail.isNotBlank() && auxEmail.contains("@")) {
                    Patterns.EMAIL_ADDRESS.matcher(auxEmail).matches()
                } else {
                    false
                }

                depositorEmail = if (validDepositorEmail) {
                    binding.etDepositorEmail.error = null
                    auxEmail
                } else {
                    binding.etDepositorEmail.error = "Correo no valido"
                    "N/A"
                }

                depositDataChange()
            }
        }

        binding.etDepositorEmail.addTextChangedListener(afterTextChangedListener)
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

                depositAmount = if (validAmount) {
                    auxAmount
                } else {
                    -1.0
                }
                depositDataChange()
            }
        }

        binding.etAmount.addTextChangedListener(afterTextChangedListener)
    }

    private fun catchResultFromDialogs() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("action")
            ?.observe(viewLifecycleOwner) { action ->
                when(action) {
                    "finishSession" -> goToLogin()
                    "deposit" -> deposit()
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

    private fun showTryAgainDepositDialog() = showNotificationDialog(
        title = "Error al depositar",
        message = "Un error inesperado ocurrió. Favor de intertarlo de nuevo.",
        bOkAction = "deposit",
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

    private fun showNoCreditFoundDialog() = showNotificationDialog(
        title = "Errorr",
        message = "Credito no encontrado",
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
        val action = DepositFragmentDirections.actionDepositFragmentToNotificationDialogFragment(
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