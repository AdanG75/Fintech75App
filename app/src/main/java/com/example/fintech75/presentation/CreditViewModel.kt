package com.example.fintech75.presentation

import android.util.Log
import androidx.lifecycle.*
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.*
import com.example.fintech75.repository.CreditRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey

class CreditViewModel(private val repo: CreditRepository): ViewModel() {

    private val _validCreditForm = MutableLiveData<Boolean>()

    fun getValidCreditForm(): LiveData<Boolean> {
        if (_validCreditForm.value == null) {
            _validCreditForm.value = false
        }

        return _validCreditForm
    }

    fun validateCreditForm(validAlias: Boolean, validEmailClient: Boolean, validAmount: Boolean) {
        _validCreditForm.value = validAlias && validEmailClient && validAmount
    }

    fun fetchCreditDetail(
        accessToken: String, idCredit: Int, privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<CreditDetail>(repo.fetchCreditDetail(
                accessToken, idCredit, privateKey
            )))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                emit(Resource.Failure(e))
            } else {
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401, 400, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 409, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun requireCreditOrder(
        accessToken: String,
        idMarket: String,
        emailClient: String,
        aliasCredit: String,
        amount: String,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        val formattedAmount: String = if (amount.contains("$")) {
            amount.trim()
        } else {
            "$${amount.trim()}"
        }

        val creditRequest = CreateCredit(
            idMarket = idMarket,
            idClient = null,
            clientEmail = emailClient,
            aliasCredit = aliasCredit,
            amount = formattedAmount
        )

        try {
            emit(Resource.Success<CreditOrderResponse>(
                repo.createCreditOrder(accessToken, creditRequest, privateKey)
            ))
        } catch (e: HttpException) {
            when (e.code()) {
                401, 400, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 409, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun performAuthCredit(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<CreditBase>(repo.performAuthCredit(
                accessToken, idOrder, fingerprintSample, GlobalSettings.notify, privateKey
            )))
        } catch (e: HttpException) {
            when (e.code()) {
                401, 400, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 409, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
                    emit(Resource.TryAgain<Unit>())
                }
            }
        } catch (e: Exception) {
            Log.d("GENERAL ERROR MESSAGE", e.message.toString())
            emit(Resource.TryAgain<Unit>())
        }
    }

    fun deleteCreditOrder(
        accessToken: String,
        idOrder: String,
        privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<BasicResponse>(repo.deleteCreditOrder(
                accessToken, idOrder, privateKey
            )))
        } catch (e: HttpException) {
            when (e.code()) {
                401, 400, 418 -> {
                    Log.d("HTTP ERROR MESSAGE (No Generic)", e.response().toString())
                    emit(Resource.Failure(checkErrorFromDetailMessage(e)))
                }
                403, 409, 404 -> {
                    Log.d("HTTP ERROR MESSAGE (Specific codes)", e.response()?.errorBody()?.string() ?: "None")
                    emit(Resource.Failure(e))
                }
                else -> {
                    Log.d("HTTP ERROR  MESSAGE", e.response().toString())
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
                401 -> {
                    when(errorParser.detail) {
                        "Couldn't validate credentials" -> {
                            val bodyResponse = "Han caducado las credenciales"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(401, bodyResponse))
                        }
                        "Session has been finished" -> {
                            val bodyResponse = "La sesi??n ha finalizado"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(401, bodyResponse))
                        }
                        "You do not have authorization to enter to this entry point" -> {
                            val bodyResponse = "No puedes usar esta tienda"
                                .toResponseBody("plain/text".toMediaTypeOrNull())
                            HttpException(Response.error<ResponseBody>(450, bodyResponse))
                        }
                        else -> {
                            e
                        }
                    }
                }
                418 -> {
                    when(errorParser.detail) {
                        "Client already has a credit within the market" -> {
                            val bodyResponse = "El cliente ya tiene un cr??dito con el establecimiento"
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


class CreditViewModelFactory(private val repo: CreditRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(CreditRepository::class.java).newInstance(repo)
    }
}
