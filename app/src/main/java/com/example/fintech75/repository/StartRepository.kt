package com.example.fintech75.repository

import com.example.fintech75.data.model.*
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

    suspend fun fetchCreditsUser(
        accessToken: String,
        userId: Int,
        userPrivateKey: PrivateKey
    ): CreditList
}