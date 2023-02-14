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
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.CreditOrderResponse
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentCreateCreditBinding
import com.example.fintech75.presentation.CreditViewModel
import com.example.fintech75.presentation.CreditViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.CreditRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey

class CreateCreditFragment : Fragment(R.layout.fragment_create_credit) {
    private val fragmentName = this::class.java.toString()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val creditViewModel: CreditViewModel by viewModels {
        CreditViewModelFactory( CreditRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentCreateCreditBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var createCreditButton: Button

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    private var aliasCredit: String = "N/A"
    private var emailClient: String = "N/A"
    private var amount: String = "0.00"

    private var validAliasCredit: Boolean = false
    private var validEmailClient: Boolean = false
    private var validAmount: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCreateCreditBinding.bind(view)
        screenLoading = binding.rlNewCreditLoading
        createCreditButton = binding.bNewCredit

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        setStateCreateCreditButton(false)

        if (currentUser.userID == -1 || currentUser.typeUser != "market") {
            showInvalidCredentialsDialog()
        } else {
            createCreditButtonStatusListener()

            addAliasEditTextListener()
            addAmountEditTextListener()
            addEmailClientEditTextListener()

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
        Log.d(fragmentName, "User ID: ${currentUser.userID}")
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }

    private fun createCreditButtonStatusListener() {
        creditViewModel.getValidCreditForm().observe(viewLifecycleOwner) { result: Boolean ->
            setStateCreateCreditButton(result)
        }
    }

    private fun creditDataChange() {
        creditViewModel.validateCreditForm(validAliasCredit, validEmailClient, validAmount)
    }

    private fun createCredit() {
        userPrivateKey?.let { privateKey: PrivateKey ->
            creditViewModel.requireCreditOrder(
                currentUser.token, currentUser.idType, emailClient, aliasCredit, amount, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Creating credit...")
                        screenLoading.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Creating credit successfully")
                        val creditResponse: CreditOrderResponse = result.data as CreditOrderResponse
                        // Log.d(fragmentName, "ID credit order: ${creditResponse.idPreCredit}")

                        goToAuthCredit(creditResponse)

                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when creating credit")
                        screenLoading.visibility = View.GONE
                        setStateCreateCreditButton(true)
                        showTryAgainCreateCreditDialog()
                    }
                    is Resource.Failure -> {
                        when((result.exception as HttpException).code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                showInvalidCredentialsDialog()
                            }
                            404 -> {
                                Log.d(fragmentName, "Client not found")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetCreditValues()

                                showNoCreditFoundDialog()
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetCreditValues()

                                showUnauthorizedCreditDialog()
                            }
                            451 -> {
                                Log.d(fragmentName, "Client have already credit with the market")
                                screenLoading.visibility = View.GONE

                                cleanEditTexts()
                                resetCreditValues()

                                showExistingCreditDialog()
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
        binding.etAliasCredit.text.clear()
        binding.etEmailClient.text.clear()
        binding.etAmount.text.clear()
    }

    private fun resetCreditValues() {
        aliasCredit = "N/A"
        emailClient = "N/A"
        amount = "0.00"

        validAliasCredit = false
        validEmailClient = false
        validAmount = false

        creditDataChange()
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

    private fun goToAuthCredit(creditResponse: CreditOrderResponse) {
        cleanEditTexts()
        resetCreditValues()

        val action = CreateCreditFragmentDirections.actionCreateCreditFragmentToAuthCreditFragment(
            aliasCredit = creditResponse.credit.aliasCredit,
            amountCredit = creditResponse.credit.amount,
            clientName = creditResponse.owners.clientName,
            marketName = creditResponse.owners.marketName,
            idOrder = creditResponse.idPreCredit
        )
        findNavController().navigate(action)
    }

    private fun setStateCreateCreditButton(isEnable: Boolean) {
        createCreditButton.isEnabled = isEnable

        if (isEnable) {
            createCreditButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            createCreditButton.setOnClickListener {
                closeKeyboard()
                createCredit()
            }

        } else {
            createCreditButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            createCreditButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun closeKeyboard() {
        GlobalSettings.inputMethodManager?.let {
            it.hideSoftInputFromWindow(binding.root.windowToken, 0)
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
                if (binding.etAmount.text.toString().matches(
                        """^\d+(\.\d{1,2})?$""".toRegex()
                    )) {
                    val auxAmount: Double = binding.etAmount.text.toString().toDouble()

                    validAmount = auxAmount > 0

                    binding.etAmount.error = if (!validAmount) {
                        amount = "0.00"
                        "La cantidad debe ser mayor a cero"
                    } else {
                        amount = binding.etAmount.text.toString()
                        null
                    }

                } else {
                    amount = "0.00"
                    validAmount= false
                    binding.etAmount.error = "Cifra no valida"
                }
                creditDataChange()
            }
        }

        binding.etAmount.addTextChangedListener(afterTextChangedListener)
    }

    private fun addAliasEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                binding.etAliasCredit.text.toString().length.let {length: Int ->
                    when {
                        length < 4 -> {
                            binding.etAliasCredit.error = "El alias debe tener al menos 4 caracteres"
                            validAliasCredit = false
                        }
                        length > 80 -> {
                            binding.etAliasCredit.error = "El alias debe ser menor a 80 caracteres"
                            validAliasCredit = false
                        }
                        else -> {
                            binding.etAliasCredit.error = null
                            aliasCredit = binding.etAliasCredit.text.toString()
                            validAliasCredit = true
                        }
                    }
                }

                creditDataChange()
            }
        }

        binding.etAliasCredit.addTextChangedListener(afterTextChangedListener)
    }

    private fun addEmailClientEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            //NO CREDITS
            override fun afterTextChanged(s: Editable) {
                val auxEmail = binding.etEmailClient.text.toString()
                validEmailClient =  if (auxEmail.isNotBlank() && auxEmail.contains("@")) {
                    Patterns.EMAIL_ADDRESS.matcher(auxEmail).matches()
                } else {
                    false
                }

                emailClient = if (validEmailClient) {
                    binding.etEmailClient.error = null
                    auxEmail
                } else {
                    binding.etEmailClient.error = "Correo no valido"
                    "N/A"
                }

                creditDataChange()
            }
        }

        binding.etEmailClient.addTextChangedListener(afterTextChangedListener)
    }

    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvNewCreditMsg.text = getString(R.string.closing_session)
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
                    "createCredit" -> createCredit()
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

    private fun showTryAgainCreateCreditDialog() = showNotificationDialog(
        title = "Crédito no creado",
        message = "No se pudo crear el crédito",
        bOkAction = "createCredit",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showNoCreditFoundDialog() = showNotificationDialog(
        title = "Error",
        message = "Cliente no encontrado",
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

    private fun showExistingCreditDialog() = showNotificationDialog(
        title = "Error",
        message = "El cliente ya posee un crédito con el establecimiento",
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
        val action = CreateCreditFragmentDirections.actionCreateCreditFragmentToNotificationDialogFragment(
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