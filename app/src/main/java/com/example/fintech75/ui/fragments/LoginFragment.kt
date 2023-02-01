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
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.EditTextResource
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.TokenBase
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentLoginBinding
import com.example.fintech75.presentation.*
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val fragmentName = this::class.java.toString()
    private val startViewModel: StartViewModel by activityViewModels {
        StartViewModelFactory(StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory(StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }
    private val loginViewModel by activityViewModels<LoginViewModel> {
        LoginViewModelFactory(StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: FragmentLoginBinding
    private lateinit var signUpButton: Button
    private lateinit var signInButton: Button
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var forgotText: TextView
    private lateinit var screenLoading: RelativeLayout

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        editUsername = binding.etLoginEmail
        editPassword = binding.etLoginPassword
        signUpButton = binding.bLoginSignUp
        signInButton = binding.bLoginSignIn
        forgotText = binding.tvLoginForgot
        screenLoading = binding.rlLoginLoading

        setupFragment()
    }

    private fun setupFragment() {
        userViewModel.setDefaultUser()
        screenLoading.visibility = View.VISIBLE
        setButtonDisable(signInButton)
        addEditTextListener()
        catchResultFromDialogs()

        getStatusStartSetup()
        validateLoginField()

        signInButton.setOnClickListener {
            closeKeyboard()
            login()
        }

        signUpButton.setOnClickListener {
            closeKeyboard()
            val action = LoginFragmentDirections.actionLoginFragmentToSignUpFragment()
            findNavController().navigate(action)
        }
    }

    private fun getStatusStartSetup() {
        startViewModel.getStartSetupState().observe(viewLifecycleOwner) { state ->
            Log.d(fragmentName, state)
            when(state) {
                AppConstants.MESSAGE_STATE_NONE -> startSetup()
                AppConstants.MESSAGE_STATE_SUCCESS -> {
                    Log.d(fragmentName, "Start has finished")
                    screenLoading.visibility = View.GONE
                    setButtonEnable(signUpButton, "secondary")
                    setForgotTextAction(enable = true)
                }
                AppConstants.MESSAGE_STATE_TRY_AGAIN -> {
                    Log.d(fragmentName, "Try to charge the server's public keys again")
                    disableItems()
                    screenLoading.visibility = View.GONE
                    showDialogTryAgainSetup()
                }
                AppConstants.MESSAGE_STATE_FAILURE -> {
                    Log.d(fragmentName, "Couldn't fetch server's public key")
                    disableItems()
                    screenLoading.visibility = View.GONE
                    showDialogTryAgainSetup()
                }
                AppConstants.MESSAGE_STATE_LOADING -> {
                    disableItems()
                    screenLoading.visibility = View.VISIBLE
                }
                else -> Log.d(fragmentName, "Unexpected state")
            }
        }
    }

    private fun startSetup() {
        startViewModel.setupStart().observe(viewLifecycleOwner) { result: Resource<*> ->
            when(result) {
                is Resource.Loading -> Log.d(fragmentName, "Loading start setup...")
                is Resource.Success -> Log.d(fragmentName, "Start setup has finished")
                is Resource.TryAgain -> Log.w(fragmentName, "An unexpected error occurs. Try to fetch the server's public keys again.")
                is Resource.Failure -> Log.e(fragmentName, "An error occurs when fetching the server's public keys")
            }
        }
    }

    private fun setButtonEnable(myButton: Button, type: String) {
        myButton.isEnabled = true

        when (type) {
            "primary" -> myButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            "secondary" -> myButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_secondary_button, context?.theme)
            else -> myButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
        }
    }

    private fun setButtonDisable(myButton: Button) {
        myButton.isEnabled = false
        myButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
    }

    private fun setForgotTextAction(enable: Boolean = true) {
        if (enable){
            forgotText.setOnClickListener{
                closeKeyboard()
                val action = LoginFragmentDirections.actionLoginFragmentToForgotPasswordFragment()
                findNavController().navigate(action)
            }
        } else {
            forgotText.setOnClickListener {
                closeKeyboard()
                return@setOnClickListener
            }
        }
    }

    private fun validateLoginField() {
        loginViewModel.getLoginState().observe(viewLifecycleOwner) { result: Pair<*,*> ->
            val usernameField: EditTextResource = result.first as EditTextResource
            val passwordField: EditTextResource = result.second as EditTextResource

            if (usernameField.isDataValid && passwordField.isDataValid) {
                setButtonEnable(signInButton, "primary")
            } else {
                setButtonDisable(signInButton)
                if (screenLoading.visibility != View.VISIBLE) {
                    usernameField.errorMessage?.let {
                        editUsername.error = it
                    }
                    passwordField.errorMessage?.let {
                        editPassword.error = it
                    }
                } else {
                    editUsername.error = null
                    editPassword.error = null
                }
            }
        }
    }

    private fun login() {
        loginViewModel.login(editUsername.text.toString(), editPassword.text.toString()).observe(viewLifecycleOwner) { result: Resource<*> ->
            when(result) {
                is Resource.Loading -> {
                    disableItems()
                    screenLoading.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    if (result.data is TokenBase) {
                        val token = result.data
                        doLogin(token)
                    }
                }
                is Resource.TryAgain -> {
                    clearEditTexts()
                    showDialogNoValidCredentials()
                    returnView()
                }
                is Resource.Failure -> {
                    Log.d(fragmentName, result.exception.message.toString())
                    showDialogTryAgainLogin()
                    returnView()
                }
                else -> {
                    showDialogUnexpectedError()
                    returnView()
                }
            }
        }
    }

    private fun addEditTextListener() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                loginViewModel.loginDataChanged(editUsername.text.toString(), editPassword.text.toString())
            }
        }

        editUsername.addTextChangedListener(afterTextChangedListener)
        editPassword.addTextChangedListener(afterTextChangedListener)
    }

    private fun clearEditTexts() {
        editUsername.text.clear()
        editPassword.text.clear()
    }

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean
    ) {
        val action = LoginFragmentDirections.actionLoginFragmentToNotificationDialogFragment(
            title = title,
            msg = message,
            bOkAction = bOkAction,
            bOkText = bOkText,
            bOkAvailable = bOkAvailable,
            bCancelAction = bCancelAction,
            bCancelText = bCancelText,
            bCancelAvailable = bCancelAvailable
        )
        findNavController().navigate(action)
    }

    private fun returnView() {
        screenLoading.visibility = View.GONE
        setButtonEnable(signInButton, "primary")
        setButtonEnable(signUpButton, "secondary")
        setForgotTextAction(enable = true)
    }

    private fun disableItems() {
        setButtonDisable(signInButton)
        setButtonDisable(signUpButton)
        setForgotTextAction(enable = false)
    }

    private fun closeKeyboard() {
        GlobalSettings.inputMethodManager?.let {
            it.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }

    private fun doLogin(token: TokenBase) {
        clearEditTexts()

        val tokenBearer = "${token.tokenType} ${token.accessToken}"
        if (!(token.typeUser == "client" || token.typeUser == "market")) {
            Log.d(fragmentName, "Type of user not supportable")
            screenLoading.visibility = View.VISIBLE
            disableItems()
            logout(tokenBearer)
        } else {
            val userCredential = UserCredential(
                token = tokenBearer,
                userID = token.userID,
                typeUser = token.typeUser,
                idType = token.idType
            )
            userViewModel.setCurrentUser(userCredential)

            returnView()
            setButtonDisable(signInButton)

            val action = LoginFragmentDirections.actionLoginFragmentToCreditsFragment()
            findNavController().navigate(action)
        }
    }

    private fun logout(userToken: String) {
        userViewModel.logout(userToken).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    binding.tvLoginMsg.text = getString(R.string.closing_session)
                    screenLoading.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Close session successfully")
                    binding.tvLoginMsg.text = getString(R.string.loading)
                    returnView()
                    setButtonDisable(signInButton)
                }
                else -> {
                    screenLoading.visibility = View.GONE
                    Log.d(fragmentName, "Un error ocurrió, favor de intentarlo de nuevo")
                    showDialogLogoutError()
                }
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
                    "startSetup" -> startSetup()
                    "login" -> login()
                    AppConstants.ACTION_CLOSE_APP -> activity?.finish()
                    else -> return@observe
                }
            }
    }

    private fun showDialogUnexpectedError() = showNotificationDialog(
        title = "Error",
        message = "Ha ocurrido un error inesperado",
        bOkAction = AppConstants.ACTION_NONE,
        bOkText = getString(R.string.try_again),
        bOkAvailable = false,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    private fun showDialogTryAgainLogin() = showNotificationDialog(
        title = "Error",
        message = "No se pudo iniciar la sesión",
        bOkAction = "login",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_NONE,
        bCancelText = getString(R.string.close),
        bCancelAvailable = true
    )

    private fun showDialogTryAgainSetup() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cargar las credenciales del sistema",
        bOkAction = "startSetup",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    private fun showDialogNoValidCredentials() = showNotificationDialog(
        title = getString(R.string.not_valid_credentials),
        message = "Correo electrónico o contraseña no validos",
        bOkAction = AppConstants.ACTION_NONE,
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_NONE,
        bCancelText = getString(R.string.close),
        bCancelAvailable = false
    )

    private fun showDialogLogoutError() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cerrar la sesión",
        bOkAction = AppConstants.ACTION_NONE,
        bOkText = getString(R.string.try_again),
        bOkAvailable = false,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    override fun onDetach() {
        super.onDetach()
        (activity as MainActivity).showBottomNavigation()
    }
}