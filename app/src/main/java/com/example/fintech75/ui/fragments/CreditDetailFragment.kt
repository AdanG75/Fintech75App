package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.example.fintech75.data.model.CreditBase
import com.example.fintech75.data.model.CreditDetail
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentCreditDetailBinding
import com.example.fintech75.presentation.CreditViewModel
import com.example.fintech75.presentation.CreditViewModelFactory
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.CreditRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import com.example.fintech75.ui.adapters.MovementAdapter
import java.security.PrivateKey

class CreditDetailFragment : Fragment(R.layout.fragment_credit_detail) {
    private val fragmentName = this::class.java.toString()
    private val args: CreditDetailFragmentArgs by navArgs()
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

    private lateinit var binding: FragmentCreditDetailBinding
    private lateinit var screenLoading: RelativeLayout
    private lateinit var movementsButton: Button

    private lateinit var creditBase: CreditBase
    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        creditBase = args.credit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCreditDetailBinding.bind(view)
        screenLoading = binding.rlCreditDetailLoading
        movementsButton = binding.bMovements

        setup()
    }

    private fun setup() {
        screenLoading.visibility = View.VISIBLE
        catchResultFromDialogs()

        if (creditBase.idCredit == -1) {
            showNoCreditPassedDialog()
        } else {
            currentUserListener()
            userPrivateKeyListener()
            refreshListener()
            setStateMovementsButton(false)

            getCreditDetail()
        }
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
            getCreditDetail()
        }
    }

    private fun setStateMovementsButton(isEnable: Boolean) {
        movementsButton.isEnabled = isEnable

        if (isEnable) {
            movementsButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_main_button, context?.theme)
            movementsButton.setOnClickListener {
                val action = CreditDetailFragmentDirections.actionCreditDetailFragmentToMovementOptionsDialogFragment(
                    userType = currentUser.typeUser,
                    creditId = creditBase.idCredit,
                    creditType = creditBase.typeCredit
                )
                findNavController().navigate(action)
            }

        } else {
            movementsButton.background = ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
            movementsButton.setOnClickListener {
                return@setOnClickListener
            }
        }
    }

    private fun getCreditDetail() {
        userPrivateKey?.let { privateKey ->
            creditViewModel.fetchCreditDetail(
                currentUser.token, creditBase.idCredit, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Loading credit's detail...")
                        screenLoading.visibility = View.VISIBLE
                        setStateMovementsButton(false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Getting credit's detail have finished successfully")
                        val creditDetail: CreditDetail = result.data as CreditDetail
                        // Log.d(fragmentName, "Credit detail owner: ${creditDetail.owner.nameOwner}")

                        bind(creditDetail)

                        setStateMovementsButton(true)
                        binding.srlRefresh.isRefreshing = false
                        screenLoading.visibility = View.GONE
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "An error occurs when fetching credit's detail. Please try again")
                        binding.srlRefresh.isRefreshing = false
                        showTryAgainCreditDetailDialog()
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

    private fun bind(creditDetail: CreditDetail) {
        binding.tvCreditAlias.text = creditDetail.credit.aliasCredit
        binding.tvCreditDate.text = creditDetail.credit.createdTime.substring(0, 10)
        binding.tvCreditAmount.text = creditDetail.credit.amount

        "ID: ${creditDetail.credit.idCredit}".also {
            binding.tvCreditId.text = it
        }
        "Tipo: ${creditDetail.credit.typeCredit}".also {
            binding.tvCreditType.text = it
        }


        val owner = if (currentUser.typeUser == "client") {
            "Tienda:"
        } else {
            "Cliente:"
        }
        "$owner ${creditDetail.owner.nameOwner}".also {
            binding.tvCreditOwner.text = it
        }
        binding.tvCreditOwnerType.text = if (currentUser.typeUser == "client") {
            "(Emisor)"
        } else {
            "(Solicitante)"
        }

        binding.rvCreditDetail.adapter = MovementAdapter(creditDetail.movements)

        if (creditDetail.movements.isEmpty()) {
            binding.rvCreditDetail.visibility = View.GONE
            binding.cvWithoutMovements.visibility = View.VISIBLE
        } else {
            binding.rvCreditDetail.visibility = View.VISIBLE
            binding.cvWithoutMovements.visibility = View.GONE
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
                    binding.tvCreditDetailMsg.text = getString(R.string.closing_session)
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
                    "getCreditDetail" -> getCreditDetail()
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

    private fun showTryAgainCreditDetailDialog() = showNotificationDialog(
        title = "Crédito no cargado",
        message = "No se pudo cargar el detalle del crédito",
        bOkAction = "getCreditDetail",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true
    )

    private fun showNoCreditPassedDialog() = showNotificationDialog(
        title = "Error",
        message = "No se encontró el crédito proporcionado",
        bOkAction = "return",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false,
        closeAction = "closeFragment"
    )

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean, closeAction: String = "none"
    ) {
        val action = CreditDetailFragmentDirections.actionCreditDetailFragmentToNotificationDialogFragment(
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