package com.example.fintech75.repository

import com.example.fintech75.data.model.BasicResponse
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.data.model.TokenBase

interface StartRepository {
    suspend fun getPublicKeyServer(): PEMData

    suspend fun login(username: String, password: String): TokenBase

    suspend fun logout(accessToken: String): BasicResponse
}