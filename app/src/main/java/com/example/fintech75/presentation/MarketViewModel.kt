package com.example.fintech75.presentation

import androidx.lifecycle.*
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.MarketsList
import com.example.fintech75.repository.MarketRepository
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import java.security.PrivateKey

class MarketViewModel(private val repo: MarketRepository): ViewModel() {

    fun fetchAllMarkets(
        token: String, privateKey: PrivateKey
    ) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            emit(Resource.Success<MarketsList>(repo.fetchAllMarkets(token, privateKey)))
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

class MarketViewModelFactory(private val repo: MarketRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(MarketRepository::class.java).newInstance(repo)
    }
}