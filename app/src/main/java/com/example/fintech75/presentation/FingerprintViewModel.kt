package com.example.fintech75.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FingerprintViewModel: ViewModel() {

    fun activateTimer() = liveData<Boolean>(viewModelScope.coroutineContext + Dispatchers.Main) {
        emit(false)
        withContext(Dispatchers.IO) {
            Thread.sleep(45000L)
        }
        emit(true)
    }

}