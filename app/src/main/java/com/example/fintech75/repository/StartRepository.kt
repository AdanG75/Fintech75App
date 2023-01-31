package com.example.fintech75.repository

import com.example.fintech75.data.model.BasicResponse
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.data.model.TokenBase
import java.security.PrivateKey
import java.security.PublicKey

interface StartRepository {
    suspend fun getPublicKeyServer(): PEMData

    suspend fun login(username: String, password: String): TokenBase

    suspend fun logout(accessToken: String): BasicResponse

    suspend fun sendUserPublicKey(
        accessToken: String,
        userId: Int,
        userPublicKey: PublicKey,
        userPrivateKey: PrivateKey
    ): BasicResponse

    suspend fun haveUserRegisteredFingerprint(
        accessToken: String,
        userId: Int,
        userType: String,
        userPrivateKey: PrivateKey
    ): BasicResponse
}