package com.example.fintech75.data.remote

import com.example.fintech75.data.model.BasicResponse
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.data.model.TokenBase

class RemoteDataSource(private val webService: WebService) {
    suspend fun getPublicKeyServer(): PEMData {
        return webService.getPublicKeyServer()
    }

    suspend fun login(username: String, password: String, secure: Boolean = true): TokenBase {
        return webService.login(username, password, secure = secure)
    }

    suspend fun logout(accessToken: String): BasicResponse {
        return webService.logout(auth = accessToken)
    }
}