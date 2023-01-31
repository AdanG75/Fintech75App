package com.example.fintech75.presentation

import androidx.lifecycle.*
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.BasicResponse
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.repository.StartRepository
import com.example.fintech75.secure.RSASecure
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import java.security.Key
import java.security.PrivateKey
import java.security.PublicKey

class UserViewModel(private val repo: StartRepository): ViewModel() {
    private val _currentUser = MutableLiveData<UserCredential>()
    private val _userPrivateKey = MutableLiveData<PrivateKey?>()
    private val _userPublicKey = MutableLiveData<PublicKey?>()
    private val _userSetupStatus = MutableLiveData<String>()

    private val defaultUser: UserCredential = UserCredential(
        token = "N/A",
        userID = -1,
        typeUser = "N/A",
        idType = "N/A"
    )

    fun setCurrentUser(loggedUser: UserCredential){
        _currentUser.value = loggedUser
    }

    fun getCurrentUser(): LiveData<UserCredential>{
        if (_currentUser.value == null) {
            _currentUser.value = defaultUser
        }

        return _currentUser
    }

    fun getUserSetupStatus(): LiveData<String> {
        if (_userSetupStatus.value == null) {
            _userSetupStatus.value = AppConstants.MESSAGE_STATE_NONE
        }

        return _userSetupStatus
    }

    fun getUserPrivateKey(): LiveData<PrivateKey?> = _userPrivateKey

    fun getUserPublicKey(): LiveData<PublicKey?> = _userPublicKey

    fun logout(token: String) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())
        try {
            val result = Resource.Success<BasicResponse>(repo.logout(token))
            _currentUser.value = defaultUser
            _userPrivateKey.value = null
            _userPublicKey.value = null
            _userSetupStatus.value = AppConstants.MESSAGE_STATE_NONE

            emit(result)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                emit(Resource.Failure(e))
            } else {
                emit(Resource.TryAgain<Unit>())
            }
        } catch (e: HttpException) {
            emit(Resource.Failure(e))
        }
    }

    fun generateUserKeys() = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())

        try {
            val result = Resource.Success<Map<String, Key>>(RSASecure.generatePublicAndPrivateKey())

            val keysMap = result.data
            keysMap["privateKey"]?.let {
                _userPrivateKey.value = it as PrivateKey
            }
            keysMap["publicKey"]?.let {
                _userPublicKey.value = it as PublicKey
            }

            emit(result)
        } catch (e: Exception) {
            emit(Resource.Failure(e))
        }
    }

    fun setDefaultUser() {
        _currentUser.value = defaultUser
    }
}

class UserViewModelFactory(private val repo: StartRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(StartRepository::class.java).newInstance(repo)
    }
}
