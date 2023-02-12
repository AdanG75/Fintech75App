package com.example.fintech75.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
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

    fun getValidForm(): LiveData<Boolean> {
        if (_validPayForm.value == null) {
            _validPayForm.value = false
        }

        return _validPayForm
    }

    fun validatePayForm(creditValid: Boolean, marketValid: Boolean, amountValid: Boolean) {
        _validPayForm.value = creditValid && marketValid && amountValid
    }

    fun generatePaySummary(
        token: String, payMethod: String, idCredit: Int, idMarket: String, amount: Double, privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())
        val creditValue: Int = if (payMethod == "paypal") {
            1
        } else {
            idCredit
        }

        val movementRequest = MovementTypeRequest(
            idCredit = creditValue,
            typeMovement = "payment",
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
                repo.generateMovementSummary(token, movementRequest, "payment", privateKey)
            ))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403 || e.code() == 409) {
                Log.d("HTTP ERROR MESSAGE (Mo Generic)", e.response().toString())
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

    fun beginMovement(
        token: String, movementRequest: MovementExtraRequest, userPrivateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<MovementComplete>(
                repo.beginMovement(token, movementRequest, movementRequest.typeMovement, userPrivateKey)
            ))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403 || e.code() == 409) {
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
