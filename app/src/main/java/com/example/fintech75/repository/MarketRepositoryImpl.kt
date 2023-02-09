package com.example.fintech75.repository

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.InternetCheck
import com.example.fintech75.data.model.CreditMarketClient
import com.example.fintech75.data.model.MarketsList
import com.example.fintech75.data.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey

class MarketRepositoryImpl(private val remoteDataSource: RemoteDataSource): MarketRepository {

    override suspend fun fetchAllMarkets(
        accessToken: String,
        userPrivateKey: PrivateKey
    ): MarketsList {
        val response: MarketsList = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchAllMarkets(accessToken, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun fetchMarketDetail(
        accessToken: String,
        idMarket: String,
        idUser: Int,
        userPrivateKey: PrivateKey
    ): CreditMarketClient {
        val response: CreditMarketClient= withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchMarketDetail(accessToken, idMarket, idUser, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }
}