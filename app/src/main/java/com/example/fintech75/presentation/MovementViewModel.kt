package com.example.fintech75.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.*
import com.example.fintech75.repository.MovementRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey

class MovementViewModel(private val repo: MovementRepository): ViewModel() {

    private val _validPayForm = MutableLiveData<Boolean>()
    private val _validDepositForm = MutableLiveData<Boolean>()
    private val _validTransferForm = MutableLiveData<Boolean>()
    private val _validWithdrawForm = MutableLiveData<Boolean>()

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

    fun getValidWithdrawForm(): LiveData<Boolean> {
        if (_validWithdrawForm.value == null) {
            _validWithdrawForm.value = false
        }

        return _validWithdrawForm
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

    fun validateWithdrawForm(originCreditValid: Boolean, amountValid: Boolean) {
        _validWithdrawForm.value = originCreditValid && amountValid
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
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
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
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
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
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }

    }

    fun generateWithdrawSummary(
        token: String,
        idOriginCredit: Int,
        amount: Double,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())


        val movementRequest = MovementTypeRequest(
            idCredit = idOriginCredit,
            typeMovement = AppConstants.WITHDRAW_MOVEMENT,
            amount = amount,
            typeSubMovement = AppConstants.CASH_METHOD,
            destinationCredit = null,
            idMarket = null,
            depositorName = null,
            depositorEmail = null,
            typeTransfer = null
        )

        try {
            emit(Resource.Success<MovementExtraRequest>(
                repo.generateMovementSummary(token, movementRequest, AppConstants.WITHDRAW_MOVEMENT, privateKey)
            ))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
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
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun executeMovement(
        accessToken: String, idMovement: Int, userPrivateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<MovementComplete>(repo.executeMovement(
                accessToken, idMovement, GlobalSettings.notify, userPrivateKey
            )))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun cancelMovement(
        accessToken: String, idMovement: Int, userPrivateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<BasicResponse>(repo.cancelMovement(
                accessToken, idMovement, GlobalSettings.notify, userPrivateKey
            )))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun performAuthMovement(
        accessToken: String,
        idMovement: Int,
        fingerprintSample: FingerprintSample,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<MovementComplete>(repo.performAuthFingerprintMovement(
                accessToken, idMovement, fingerprintSample, GlobalSettings.notify, privateKey
            )))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 401, 409, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    Log.d("HTTP ERROR  MESSAGE (body)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.TryAgain<Unit>())
                }
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

            when(e.code()) {
                400 -> {
                    when(errorParser.detail) {
                        "Unsupportable user type. It is not compatible with the operation" -> {
                            val bodyResponse = "No puedes usar este cŕedito"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(450, bodyResponse))
                        }
                        else -> {
                            e
                        }
                    }
                }
                401 -> {
                    when(errorParser.detail) {
                        "Couldn't validate credentials" -> {
                            val bodyResponse = "Han caducado las credenciales"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(401, bodyResponse))
                        }
                        "Session has been finished" -> {
                            val bodyResponse = "La sesión ha finalizado"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(401, bodyResponse))
                        }
                        "You do not have authorization to enter to this entry point" -> {
                            val bodyResponse = "No puedes usar este cŕedito"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(450, bodyResponse))
                        }
                        else -> {
                            e
                        }
                    }
                }
                403 -> {
                    when(errorParser.detail) {
                        "Operation expired. Please generate a new one" -> {
                            val bodyResponse = "La operación ha finalizado"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(403, bodyResponse))
                        }
                        else -> {
                            e
                        }
                    }
                }
                404 -> {
                    when(errorParser.detail) {
                        "Element not found" -> {
                            val bodyResponse = "Objeto no encontrado"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(404, bodyResponse))
                        }
                        else -> {
                            e
                        }
                    }
                }
                409 -> {
                    when(errorParser.detail) {
                        "Insufficient funds" -> {
                            val bodyResponse = "Fondos insuficientes"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(409, bodyResponse))
                        }
                        else -> {
                            val bodyResponse = "Error al procesar el movimiento"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(460, bodyResponse))
                        }
                    }
                }
                418 -> {
                    when(errorParser.detail) {
                        "Client already has a credit within the market" -> {
                            val bodyResponse = "El cliente ya tiene un crédito con el establecimiento"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(451, bodyResponse))
                        }
                        "Fingerprint could not be created", "Reconstruction fingerprint failed" -> {
                            val bodyResponse = "No se pudo reconstruir la huella"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(480, bodyResponse))
                        }
                        "All fingerprint samples are of low quality. Please capture new samples" -> {
                            val bodyResponse = "Ninguna huella fue aceptada para el registro"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(481, bodyResponse))
                        }
                        "Poor quality fingerprint", "Few minutiae have been finding" -> {
                            val bodyResponse = "Mala calidad de la huella"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(482, bodyResponse))
                        }
                        "Fingerprints do not match" -> {
                            val bodyResponse = "Huella no emparejada"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(483, bodyResponse))
                        }

                        else -> e
                    }
                }

                else -> e
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
