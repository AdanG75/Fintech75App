package com.example.fintech75.repository

import com.example.fintech75.data.model.*
import java.security.PrivateKey

interface CreditRepository {

    suspend fun fetchCreditDetail(
        accessToken: String,
        idCredit: Int,
        userPrivateKey: PrivateKey
    ): CreditDetail

    suspend fun createCreditOrder(
        accessToken: String,
        creditRequest: CreateCredit,
        userPrivateKey: PrivateKey
    ): CreditOrderResponse

    suspend fun saveCreditFingerprint(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        userPrivateKey: PrivateKey
    ): BasicResponse

    suspend fun authCreditOrder(
        accessToken: String,
        idOrder: String,
        userPrivateKey: PrivateKey
    ): BasicResponse

    suspend fun createCredit(
        accessToken: String,
        idOrder: String,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): CreditBase

    suspend fun deleteCreditOrder(
        accessToken: String,
        idOrder: String,
        userPrivateKey: PrivateKey
    ): BasicResponse

    suspend fun performAuthCredit(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): CreditBase

}