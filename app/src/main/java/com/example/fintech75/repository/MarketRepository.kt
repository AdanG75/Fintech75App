package com.example.fintech75.repository

import com.example.fintech75.data.model.MarketsList
import java.security.PrivateKey

interface MarketRepository {

    suspend fun fetchAllMarkets(
        accessToken: String,
        userPrivateKey: PrivateKey
    ): MarketsList
}