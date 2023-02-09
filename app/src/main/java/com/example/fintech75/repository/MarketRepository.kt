package com.example.fintech75.repository

import com.example.fintech75.data.model.CreditMarketClient
import com.example.fintech75.data.model.MarketsList
import java.security.PrivateKey

interface MarketRepository {

    suspend fun fetchAllMarkets(
        accessToken: String,
        userPrivateKey: PrivateKey
    ): MarketsList

    suspend fun fetchMarketDetail(
        accessToken: String,
        idMarket: String,
        idUser: Int,
        userPrivateKey: PrivateKey
    ): CreditMarketClient
}