package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.example.fintech75.databinding.FragmentPayBinding
import com.example.fintech75.presentation.*
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey

class PayFragment : Fragment(R.layout.fragment_pay), AdapterView.OnItemSelectedListener {
    private val fragmentName = this::class.java.toString()
    private val args: PayFragmentArgs by navArgs()
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

    private lateinit var binding: FragmentPayBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var payButton: Button
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null
    private var userGlobalCredit: Int? = null

    private var payMethod: String = AppConstants.CREDIT_METHOD
    private var payOriginCredit: Int = -1
    private var payMarketId: String = "N/A"
    private var payAmount: Double = -1.0

    private var validOriginCredit: Boolean = false
    private var validMarketId: Boolean = false
    private var validAmount: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(fragmentName, "Credit ID: ${args.currentCreditId}, Credit type: ${args.currentCreditType}, Market ID: ${args.marketId}")

        binding = FragmentPayBinding.bind(view)
        screenLoading = binding.rlPayLoading
        payButton = binding.bPay

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()
        userGlobalCreditListener()

        catchResultFromDialogs()
        setStatePayButton(false)

        if (currentUser.userID == -1) {
            showInvalidCredentialsDialog()
        } else {

            if (currentUser.typeUser == "market") {
                payMethod = AppConstants.CREDIT_METHOD
                binding.cvPayMethod.visibility = View.GONE
                binding.cvCredit.visibility = View.VISIBLE
            } else {
                radioButtonListener()

            }

            configCreditView()
            setScreenView(currentUser.typeUser)

            addAmountEditTextListener()
            addCreditEditTextListener()
            addMarketEditTextListener()
            payButtonStatusListener()

            screenLoading.visibility = View.GONE
        }
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A"
        )
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun userGlobalCreditListener() {
        userGlobalCredit = userViewModel.getUserGlobalCredit().value
    }

    private fun radioButtonListener() {
        binding.rgPayMethod.setOnCheckedChangeListener { _, idChecked ->
            payMethod = when (idChecked) {
                R.id.rb_pay_method_credit -> {
                    cleanEditTexts()
                    resetPayValues()
                    payDataChange()
                    setScreenView(currentUser.typeUser)
                    configCreditView()
                    binding.cvCredit.visibility = View.VISIBLE
                    AppConstants.CREDIT_METHOD
                }
                R.id.rb_pay_method_paypal -> {
                    binding.cvCredit.visibility = View.GONE
                    cleanEditTexts()
                    resetPayValues()
                    payDataChange()
                    setScreenView(currentUser.typeUser)
                    validOriginCredit = true
                    AppConstants.PAYPAL_METHOD
                }
                else -> {
                    cleanEditTexts()
                    resetPayValues()
                    payDataChange()
                    setScreenView(currentUser.typeUser)
                    configCreditView()
                    binding.cvCredit.visibility = View.VISIBLE
                    AppConstants.CREDIT_METHOD
                }
            }
        }
    }

    private fun configCreditView() {
        if (userGlobalCredit == null && args.currentCreditId == -1) {
            spinnerAdapter = ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf<String>("Sin créditos")
            )

            binding.sCredits.visibility = View.GONE
            binding.etCredit.visibility = View.VISIBLE

            payOriginCredit = -1
            validOriginCredit = false
        } else {
            spinnerAdapter = ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item
            )

            if (args.currentCreditId != -1) {
                spinnerAdapter.add("${args.currentCreditId} (${args.currentCreditType})")
                payOriginCredit = args.currentCreditId
            }

            if (args.currentCreditType != "global" && userGlobalCredit != null) {
                spinnerAdapter.add("$userGlobalCredit (global)")

                if (args.currentCreditId == -1) {
                    userGlobalCredit?.let {
                        payOriginCredit = it
                    }
                }
            }

            binding.sCredits.visibility = View.VISIBLE
            binding.etCredit.visibility = View.GONE

            validOriginCredit = true
        }
        binding.sCredits.onItemSelectedListener = this
        binding.sCredits.adapter = spinnerAdapter
        payDataChange()
    }

    private fun setScreenView(typeUser: String) {
        when(typeUser) {
            "market" -> {
                binding.tvMarket.text = currentUser.idType
                payMarketId = currentUser.idType
                validMarketId = true

                binding.tvMarket.visibility = View.VISIBLE
                binding.etMarket.visibility = View.GONE
            }
            "client" -> {
                if (args.marketId != "N/A") {
                    binding.tvMarket.text = args.marketId
                    payMarketId = args.marketId
                    validMarketId = true

                    binding.tvMarket.visibility = View.VISIBLE
                    binding.etMarket.visibility = View.GONE
                } else {
                    payMarketId = "N/A"
                    validMarketId = false

                    binding.tvMarket.visibility = View.GONE
                    binding.etMarket.visibility = View.VISIBLE
                }
            }
            else -> showInvalidCredentialsDialog()
        }
        payDataChange()
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
                if (binding.etCredit.text.toString().matches(
                        """^\d+$""".toRegex()
                )) {
                    payOriginCredit = binding.etCredit.text.toString().toInt()
                    validOriginCredit = payOriginCredit > 0

                    binding.etCredit.error = if (!validOriginCredit) {
                        payOriginCredit = -1
                        "El crédito debe ser mayor a cero"
                    } else {
                        null
                    }

                } else {
                    payOriginCredit = -1
                    validOriginCredit = false
                    binding.etCredit.error = "Formato de crédito no valido"
                }
                payDataChange()
            }
        }

        binding.etCredit.addTextChangedListener(afterTextChangedListener)
    }

    private fun addMarketEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                if (binding.etMarket.text.toString().matches(
                        """^MKT-[a-f0-9]{32}$""".toRegex()
                )) {
                    payMarketId = binding.etMarket.text.toString()
                    validMarketId = true
                    binding.etMarket.error = null
                } else {
                    payMarketId = "N/A"
                    validMarketId = false
                    binding.etMarket.error = "ID de la tienda no valido"
                }
                payDataChange()
            }
        }

        binding.etMarket.addTextChangedListener(afterTextChangedListener)
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
                if (binding.etAmount.text.toString().matches(
                        """^\d+(\.\d{1,2})?$""".toRegex()
                    )) {
                    payAmount = binding.etAmount.text.toString().toDouble()
                    validAmount = payAmount > 0

                    binding.etAmount.error = if (!validAmount) {
                        payAmount = -1.0
                        "La cantidad debe ser mayor a cero"
                    } else {
                        null
                    }

                } else {
                    payAmount = -1.0
                    validAmount= false
                    binding.etAmount.error = "Cifra no valida"
                }
                payDataChange()
            }
        }

        binding.etAmount.addTextChangedListener(afterTextChangedListener)
    }

    private fun payButtonStatusListener() {
        movementViewModel.getValidPaymentForm().observe(viewLifecycleOwner) { result: Boolean ->
            setStatePayButton(result)
        }
    }

    private fun payDataChange() {
        movementViewModel.validatePayForm(validOriginCredit, validMarketId, validAmount)
    }

    private fun cleanEditTexts() {
        binding.etAmount.text.clear()
        binding.etCredit.text.clear()
        binding.etMarket.text.clear()
    }

    private fun resetPayValues() {
        payOriginCredit = -1
        payMarketId = "N/A"
        payAmount = -1.0

        validOriginCredit = false
        validMarketId = false
        validAmount = false
    }

    private fun setStatePayButton(isEnable: Boolean) {
        payButton.isEnabled = isEnable

        if (isEnable) {
            payButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            payButton.setOnClickListener {
                closeKeyboard()
                pay()
            }

        } else {
            payButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            payButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun pay() {
        Log.d(fragmentName, "Pay function")
        Log.d(fragmentName, "Pay method: $payMethod, Credit: $payOriginCredit, Market: $payMarketId, Amount: $payAmount")
        userPrivateKey?.let { privateKey: PrivateKey ->
            movementViewModel.generatePaySummary(
                currentUser.token, payMethod, payOriginCredit, payMarketId, payAmount, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating movement's summary...")
                        screenLoading.visibility = View.VISIBLE
                        setStatePayButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting movement's summary")
                        val movementSummary: MovementExtraRequest = result.data as MovementExtraRequest

                        goToMovementSummary(movementSummary)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when generating movement's summary")
                        screenLoading.visibility = View.GONE
                        setStatePayButton(true)
                        showTryAgainPayDialog()
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
                                resetPayValues()
                                payDataChange()

                                showNoCreditFoundDialog()
                            }
                            409 -> {
                                Log.d(fragmentName, "Not enough funds")
                                screenLoading.visibility = View.GONE
                                setStatePayButton(true)
                                showNotEnoughFundsDialog()
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetPayValues()
                                payDataChange()

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
        resetPayValues()
        val action = PayFragmentDirections.actionPayFragmentToMovementSummaryFragment(
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
                    binding.tvPayMsg.text = getString(R.string.closing_session)
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
                    "pay" -> pay()
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

    private fun showTryAgainPayDialog() = showNotificationDialog(
        title = "Error al pagar",
        message = "Un error inesperado ocurrió. Favor de intertarlo de nuevo.",
        bOkAction = "pay",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showNotEnoughFundsDialog() = showNotificationDialog(
        title = "Error al pagar",
        message = "Fondos insuficientes.",
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

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean, closeAction: String = "none"
    ) {
        val action = PayFragmentDirections.actionPayFragmentToNotificationDialogFragment(
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

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        val creditSelectedText = spinnerAdapter.getItem(position)
        val creditParts = creditSelectedText?.split("""\s""".toRegex()) ?: listOf()

        val creditToSend = if (creditParts.isNotEmpty()) {
            if (creditParts[0].matches("""^\d+$""".toRegex())) {
                validOriginCredit = true
                creditParts[0].toInt()
            } else {
                validOriginCredit = false
                -1
            }
        } else {
            validOriginCredit = false
            -1
        }

        payOriginCredit = creditToSend

        payDataChange()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        return Unit
    }
}