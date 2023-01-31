package com.example.fintech75.presentation

import androidx.lifecycle.*
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.repository.StartRepository
import com.example.fintech75.secure.RSASecure
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import java.security.Key
import java.security.PrivateKey
import java.security.PublicKey

class StartViewModel(private val repo: StartRepository): ViewModel() {
    private val _publicServerKey = MutableLiveData<PublicKey?>()
    private val _serverKeyState = MutableLiveData<String>()
    private val _mobileKeys = MutableLiveData<Map<String, Key>?>()
    private val _mobileKeysState = MutableLiveData<String>()
    private val _startSetupState = MutableLiveData<String>()

    fun getPublicServerKey(): LiveData<PublicKey?> = _publicServerKey

    fun getMobileKeys(): LiveData<Map<String, Key>?> = _mobileKeys

    fun getStartSetupState(): LiveData<String> {
        if (_startSetupState.value == null) {
            _startSetupState.value = AppConstants.MESSAGE_STATE_NONE
        }

        return _startSetupState
    }

    fun getPublicServerKeyState(): LiveData<String> {
        if (_serverKeyState.value == null) {
            _serverKeyState.value = AppConstants.MESSAGE_STATE_NONE
        }

        return _serverKeyState
    }

    fun getMobileKeysState(): LiveData<String> {
        if (_mobileKeysState.value == null) {
            _mobileKeysState.value = AppConstants.MESSAGE_STATE_NONE
        }

        return _mobileKeysState
    }

    fun fetchPublicKeyServer() = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        _serverKeyState.value = AppConstants.MESSAGE_STATE_LOADING
        emit(Resource.Loading<Unit>())

        try {
            val result = Resource.Success<PEMData>(repo.getPublicKeyServer())
            val publicServerKey = RSASecure.loadPublicKeyFromPEM(result.data.pem) as PublicKey

            GlobalSettings.severPublicKey = publicServerKey
            _publicServerKey.value = publicServerKey
            _serverKeyState.value = AppConstants.MESSAGE_STATE_SUCCESS

            emit(result)
        } catch (e: HttpException) {
            _serverKeyState.value = AppConstants.MESSAGE_STATE_TRY_AGAIN
            emit(Resource.TryAgain<Unit>())
        } catch (e: Exception) {
            _serverKeyState.value = AppConstants.MESSAGE_STATE_FAILURE
            emit(Resource.Failure(e))
        }
    }

    fun generateMobileKeys() = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        _mobileKeysState.value = AppConstants.MESSAGE_STATE_LOADING
        emit(Resource.Loading<Unit>())
        try {
            val result = Resource.Success<Map<String, Key>>(RSASecure.generatePublicAndPrivateKey())
            _mobileKeys.value = result.data

            val keysMap = result.data
            keysMap["privateKey"]?.let {
                GlobalSettings.mobilePrivateKey = it as PrivateKey
            }
            keysMap["publicKey"]?.let {
                GlobalSettings.mobilePublicKey = it as PublicKey
            }
            _mobileKeysState.value = AppConstants.MESSAGE_STATE_SUCCESS

            emit(result)
        } catch (e: Exception) {
            _mobileKeysState.value = AppConstants.MESSAGE_STATE_FAILURE
            emit(Resource.Failure(e))
        }
    }

    fun setupStart() = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        _startSetupState.value = AppConstants.MESSAGE_STATE_LOADING
        emit(Resource.Loading<Unit>())
        try {
            val result = Resource.Success<Pair<PEMData, Map<String, Key>>>(
                Pair(repo.getPublicKeyServer(), RSASecure.generatePublicAndPrivateKey())
            )
            val (pemServer, keysMap) = result.data

            val publicServerKey = RSASecure.loadPublicKeyFromPEM(pemServer.pem) as PublicKey
            GlobalSettings.severPublicKey = publicServerKey
            _publicServerKey.value = publicServerKey

            _mobileKeys.value = keysMap
            keysMap["privateKey"]?.let {
                GlobalSettings.mobilePrivateKey = it as PrivateKey
            }
            keysMap["publicKey"]?.let {
                GlobalSettings.mobilePublicKey = it as PublicKey
            }
            _startSetupState.value = AppConstants.MESSAGE_STATE_SUCCESS

            emit(result)
        } catch (e: HttpException) {
            _startSetupState.value = AppConstants.MESSAGE_STATE_TRY_AGAIN
            emit(Resource.TryAgain<Unit>())
        } catch (e: Exception) {
            _startSetupState.value = AppConstants.MESSAGE_STATE_FAILURE
            emit(Resource.Failure(e))
        }
    }

    fun setDefaultValues() {
        _publicServerKey.value = null
        _mobileKeys.value = null

        GlobalSettings.severPublicKey = null
        GlobalSettings.mobilePrivateKey = null
        GlobalSettings.mobilePublicKey = null
        GlobalSettings.notify = true
    }
}

class StartViewModelFactory(private val repo: StartRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(StartRepository::class.java).newInstance(repo)
    }
}