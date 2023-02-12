package com.example.fintech75.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.MovementTypeRequest
import com.example.fintech75.repository.MovementRepository
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
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
//        val str = "Id credit: ${movementRequest.idCredit}\nType movement: ${movementRequest.typeMovement}\n" +
//                "Amount: ${amount}\nSub:${movementRequest.typeSubMovement}\n" +
//                "Dest credit:${movementRequest.destinationCredit}\nMarket: ${movementRequest.idMarket}\n" +
//                "Dep name: ${movementRequest.depositorName}\nDep email: ${movementRequest.depositorEmail}\n" +
//                "typeTrans: ${movementRequest.typeTransfer}"
//        Log.d("Summary type", str)

        try {
            emit(Resource.Success<MovementExtraRequest>(
                repo.generateMovementSummary(token, movementRequest, "payment", privateKey)
            ))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403 || e.code() == 409) {
                emit(Resource.Failure(e))
            } else {
                // Log.d("HTTP message", e.message.toString())
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
}


class MovementViewModelFactory(private val repo: MovementRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(MovementRepository::class.java).newInstance(repo)
    }
}
