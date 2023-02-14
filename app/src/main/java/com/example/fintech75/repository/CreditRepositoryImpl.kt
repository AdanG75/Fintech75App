package com.example.fintech75.repository

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.InternetCheck
import com.example.fintech75.data.model.*
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

    override suspend fun createCreditOrder(
        accessToken: String,
        creditRequest: CreateCredit,
        userPrivateKey: PrivateKey
    ): CreditOrderResponse {
        val response: CreditOrderResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.createCreditOrder(accessToken, creditRequest, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun saveCreditFingerprint(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.saveCreditFingerprint(accessToken, idOrder, fingerprintSample, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun authCreditOrder(
        accessToken: String,
        idOrder: String,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.authCreditOrder(accessToken, idOrder, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun createCredit(
        accessToken: String,
        idOrder: String,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): CreditBase {
        val response: CreditBase = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.createCredit(accessToken, idOrder, notify, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun deleteCreditOrder(
        accessToken: String,
        idOrder: String,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.deleteCreditOrder(accessToken, idOrder, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun performAuthCredit(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): CreditBase {
        lateinit var processException: HttpException
        var occursError = false

        val response: CreditBase = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                // Send fingerprint to authorize credit's creation
                withContext(Dispatchers.Default) {
                    try {
                        remoteDataSource.saveCreditFingerprint(accessToken, idOrder, fingerprintSample, GlobalSettings.secure, userPrivateKey)
                    } catch (e: HttpException) {
                        processException = e
                        occursError = true
                        throw e
                    }
                }

                // Authorize credit if fingerprint sample has been accepted
                if (!occursError) {
                    withContext(Dispatchers.Default) {
                        try {
                            remoteDataSource.authCreditOrder(accessToken, idOrder, GlobalSettings.secure, userPrivateKey)
                        } catch (e: HttpException) {
                            processException = e
                            occursError = true
                            throw e
                        }
                    }
                }

                // Create credit
                val createdCreditResponse = withContext(Dispatchers.Default) {
                    if (occursError) {
                        CreditBase(
                            idClient = "N/A", idMarket = "N/A", idAccount = -1, aliasCredit = "N/A",
                            typeCredit = "N/A", amount = "N/A", isApproved = false, idCredit = -1,
                            inProcess = false, createdTime = "N/A"
                        )
                    } else {
                        try {
                            remoteDataSource.createCredit(accessToken, idOrder, notify, GlobalSettings.secure, userPrivateKey)
                        } catch (e: HttpException) {
                            processException = e
                            occursError = true
                            throw e
                        }
                    }
                }
                createdCreditResponse

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        if (occursError) {
            throw processException
        } else {
            return response
        }
    }
}