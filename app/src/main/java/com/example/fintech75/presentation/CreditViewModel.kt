package com.example.fintech75.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.CreditDetail
import com.example.fintech75.repository.CreditRepository
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import java.security.PrivateKey

class CreditViewModel(private val repo: CreditRepository): ViewModel() {

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
        } catch (e: Exception) {
            emit(Resource.TryAgain<Unit>())
        }
    }

}


class CreditViewModelFactory(private val repo: CreditRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(CreditRepository::class.java).newInstance(repo)
    }
}
