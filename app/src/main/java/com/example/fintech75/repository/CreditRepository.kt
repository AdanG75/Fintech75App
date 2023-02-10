package com.example.fintech75.repository

import com.example.fintech75.data.model.CreditDetail
import java.security.PrivateKey

interface CreditRepository {

    suspend fun fetchCreditDetail(
        accessToken: String,
        idCredit: Int,
        userPrivateKey: PrivateKey
    ): CreditDetail

}