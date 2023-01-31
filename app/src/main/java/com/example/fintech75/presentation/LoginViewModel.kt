package com.example.fintech75.presentation

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.*
import com.example.fintech75.core.EditTextResource
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.LoginModel
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.data.model.TokenBase
import com.example.fintech75.repository.StartRepository
import com.example.fintech75.secure.RSASecure
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import java.security.Key
import java.security.PublicKey

class LoginViewModel(private val repo: StartRepository): ViewModel() {
    private val _pairET = MutableLiveData<Pair<EditTextResource, EditTextResource>>()

    fun login(username: String, password: String) = liveData<Resource<*>>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(Resource.Loading<Unit>())
        try {
            val result = Resource.Success<TokenBase>(repo.login(username, password))
            emit(result)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 404) {
                emit(Resource.TryAgain<Unit>())
            } else {
                emit(Resource.Failure(e))
            }
        } catch (e: Exception) {
            emit(Resource.Failure(e))
        }
    }

    fun getLoginState(): LiveData<Pair<EditTextResource, EditTextResource>> {
        if (_pairET.value == null) {
            val defaultPair = Pair(
                EditTextResource("Correo no valido", false),
                EditTextResource("Debe contener mayúsculas, minúsculas, números y caracters especiales", false)
            )

            _pairET.value = defaultPair
        }
        return _pairET
    }

    fun loginDataChanged(username: String, password: String) {
        val usernameResult = if (isUserNameValid(username)) {
                EditTextResource(null, true)
            } else {
                EditTextResource("Correo no valido", false)
            }

        val passwordResult = if (isPasswordValid(password)) {
                EditTextResource(null, true)
            } else {
                EditTextResource("Debe contener mayúsculas, minúsculas, números y caracters especiales", false)
            }

        _pairET.value = Pair(usernameResult, passwordResult)
    }

    private fun isUserNameValid(username: String): Boolean {
        if (username.isNotBlank() || username.isNotEmpty()) {
            if (username.contains("@")) {
                return Patterns.EMAIL_ADDRESS.matcher(username).matches()
            }
        }

        return false
    }

    private fun isPasswordValid(password: String): Boolean {
        if (password.length >= 8){
            val regexPattern = """^(?=.*[a-z])(?=.*[A-Z])(?=.*?[0-9])(?=.*?[|!#${'$'}%&/()=?¡¿+\-*\\ñÑ\.:;,<>])[A-Za-z0-9|!#${'$'}%&/()=?¡¿+\-*\\ñÑ\.:;,<>]{8,32}$""".toRegex()
            return password.contains(regexPattern)
        }
        return false
    }
}

class LoginViewModelFactory(private val repo: StartRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(StartRepository::class.java).newInstance(repo)
    }
}
