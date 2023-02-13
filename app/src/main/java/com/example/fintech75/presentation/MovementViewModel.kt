package com.example.fintech75.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.DetailMessage
import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.MovementTypeRequest
import com.example.fintech75.repository.MovementRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey

class MovementViewModel(private val repo: MovementRepository): ViewModel() {

    private val _validPayForm = MutableLiveData<Boolean>()
    private val _validDepositForm = MutableLiveData<Boolean>()
    private val _validTransferForm = MutableLiveData<Boolean>()

    fun getValidPaymentForm(): LiveData<Boolean> {
        if (_validPayForm.value == null) {
            _validPayForm.value = false
        }

        return _validPayForm
    }

    fun getValidDepositForm(): LiveData<Boolean> {
        if (_validDepositForm.value == null) {
            _validDepositForm.value = false
        }

        return _validDepositForm
    }

    fun getValidTransferForm(): LiveData<Boolean> {
        if (_validTransferForm.value == null) {
            _validTransferForm.value = false
        }

        return _validTransferForm
    }

    fun validatePayForm(creditValid: Boolean, marketValid: Boolean, amountValid: Boolean) {
        _validPayForm.value = creditValid && marketValid && amountValid
    }

    fun validateDepositForm(creditValid: Boolean, nameValid: Boolean, emailValid: Boolean, amountValid: Boolean) {
        _validDepositForm.value = creditValid && nameValid && emailValid && amountValid
    }

    fun validateTransferForm(originCreditValid: Boolean, destinationCreditValid: Boolean, amountValid: Boolean) {
        _validTransferForm.value = originCreditValid && destinationCreditValid && amountValid
    }

    fun generatePaySummary(
        token: String, payMethod: String, idCredit: Int, idMarket: String, amount: Double, privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())
        val creditValue: Int = if (payMethod == "paypal") {
            AppConstants.DEFAULT_ID_CREDIT
        } else {
            idCredit
        }

        val movementRequest = MovementTypeRequest(
            idCredit = creditValue,
            typeMovement = AppConstants.PAY_MOVEMENT,
            amount = amount,
            typeSubMovement = payMethod,
            destinationCredit = null,
            idMarket = idMarket,
            depositorName = null,
            depositorEmail = null,
            typeTransfer = null
        )

        try {
            emit(Resource.Success<MovementExtraRequest>(
                repo.generateMovementSummary(token, movementRequest, AppConstants.PAY_MOVEMENT, privateKey)
            ))
        } catch (e: HttpException) {
            val errorCode = e.code()
            if (errorCode == 401 || errorCode == 403 || errorCode == 409 || errorCode == 400 || errorCode == 404) {
                Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                emit(Resource.Failure(checkErrorFromDetailMessage(e)))
            } else {
                Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }

    }

    fun generateDepositSummary(
        token: String,
        depositMethod: String,
        idDestinationCredit: Int,
        depositorName: String,
        depositorEmail: String,
        amount: Double,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())


        val movementRequest = MovementTypeRequest(
            idCredit = AppConstants.DEFAULT_ID_CREDIT,
            typeMovement = AppConstants.DEPOSIT_MOVEMENT,
            amount = amount,
            typeSubMovement = depositMethod,
            destinationCredit = idDestinationCredit,
            idMarket = null,
            depositorName = depositorName,
            depositorEmail = depositorEmail,
            typeTransfer = null
        )

        try {
            emit(Resource.Success<MovementExtraRequest>(
                repo.generateMovementSummary(token, movementRequest, AppConstants.DEPOSIT_MOVEMENT, privateKey)
            ))
        } catch (e: HttpException) {
            val errorCode = e.code()
            if (errorCode == 401 || errorCode == 403 || errorCode == 409 || errorCode == 400 || errorCode == 404) {
                Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                emit(Resource.Failure(checkErrorFromDetailMessage(e)))
            } else {
                Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }

    }

    fun generateTransferSummary(
        token: String,
        idOriginCredit: Int,
        idDestinationCredit: Int,
        amount: Double,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())


        val movementRequest = MovementTypeRequest(
            idCredit = idOriginCredit,
            typeMovement = AppConstants.TRANSFER_MOVEMENT,
            amount = amount,
            typeSubMovement = AppConstants.CREDIT_METHOD,
            destinationCredit = idDestinationCredit,
            idMarket = null,
            depositorName = null,
            depositorEmail = null,
            typeTransfer = AppConstants.PAYPAL_METHOD
        )

        try {
            emit(Resource.Success<MovementExtraRequest>(
                repo.generateMovementSummary(token, movementRequest, AppConstants.TRANSFER_MOVEMENT, privateKey)
            ))
        } catch (e: HttpException) {
            val errorCode = e.code()
            if (errorCode == 401 || errorCode == 403 || errorCode == 409 || errorCode == 400 || errorCode == 404) {
                Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                emit(Resource.Failure(checkErrorFromDetailMessage(e)))
            } else {
                Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }

    }

    fun beginMovement(
        token: String, movementRequest: MovementExtraRequest, userPrivateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<MovementComplete>(
                repo.beginMovement(token, movementRequest, movementRequest.typeMovement, userPrivateKey)
            ))
        } catch (e: HttpException) {
            val errorCode = e.code()
            if (errorCode == 401 || errorCode == 403 || errorCode == 409 || errorCode == 400 || errorCode == 404) {
                Log.d("HTTP ERROR MESSAGE (Mo Generic)", e.response().toString())
                emit(Resource.Failure(e))
            } else {
                Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    private fun checkErrorFromDetailMessage(e: HttpException): HttpException {
        val body = e.response()?.errorBody()?.string() ?: "None"
        val adapter = Gson().getAdapter(DetailMessage::class.java)

        return try {
            val errorParser = adapter.fromJson(body)
            when(errorParser.detail) {
                "Couldn't validate credentials" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "Han caducado las credenciales"
                    )
                    HttpException(Response.error<ResponseBody>(401, bodyResponse))
                }
                "Session has been finished" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "La sesión ha finalizado"
                    )
                    HttpException(Response.error<ResponseBody>(401, bodyResponse))
                }
                "Insufficient funds" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "Fondos insuficientes"
                    )
                    HttpException(Response.error<ResponseBody>(409, bodyResponse))
                }
                "You do not have authorization to enter to this entry point" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "No puedes usar este cŕedito"
                    )
                    HttpException(Response.error<ResponseBody>(450, bodyResponse))
                }
                "Unsupportable user type. It is not compatible with the operation" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "No puedes usar este cŕedito"
                    )
                    HttpException(Response.error<ResponseBody>(450, bodyResponse))
                }
                "Element not found" -> {
                    val bodyResponse = ResponseBody.create(
                        MediaType.parse("plain/text"),
                        "Objeto no encontrado"
                    )
                    HttpException(Response.error<ResponseBody>(404, bodyResponse))
                }

                else -> {
                    e
                }
            }
        } catch (exception: Exception) {
            e
        }
    }
}


class MovementViewModelFactory(private val repo: MovementRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(MovementRepository::class.java).newInstance(repo)
    }
}
