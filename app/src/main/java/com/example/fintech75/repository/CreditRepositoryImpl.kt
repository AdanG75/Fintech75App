package com.example.fintech75.repository

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.InternetCheck
import com.example.fintech75.data.model.CreditDetail
import com.example.fintech75.data.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey

class CreditRepositoryImpl(private val remoteDataSource: RemoteDataSource): CreditRepository {
    override suspend fun fetchCreditDetail(
        accessToken: String,
        idCredit: Int,
        userPrivateKey: PrivateKey
    ): CreditDetail {
        val response: CreditDetail = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchCreditDetail(accessToken, idCredit, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }
}