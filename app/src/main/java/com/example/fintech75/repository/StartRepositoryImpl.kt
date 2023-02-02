package com.example.fintech75.repository

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.InternetCheck
import com.example.fintech75.data.model.*
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.secure.RSASecure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.PrivateKey
import java.security.PublicKey

class StartRepositoryImpl (private val remoteDataSource: RemoteDataSource): StartRepository {
    override suspend fun getPublicKeyServer(): PEMData {
        val serverPEM: PEMData = withContext(Dispatchers.IO){
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if (thereIsInternetConnection) {
                remoteDataSource.getPublicKeyServer()
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return serverPEM
    }

    override suspend fun login(username: String, password: String): TokenBase {
        val tokenUser: TokenBase = withContext(Dispatchers.IO){
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if (thereIsInternetConnection) {
                if (GlobalSettings.secure) {
                    val severPublicKey: PublicKey = if (GlobalSettings.severPublicKey != null) {
                        GlobalSettings.severPublicKey as PublicKey
                    } else {
                        throw Exception("No key has been loaded")
                    }

                    val secureUsername = RSASecure.encryptMessage(username, severPublicKey)
                    val securePassword = RSASecure.encryptMessage(password, severPublicKey)

                    remoteDataSource.login(secureUsername, securePassword, true)
                } else {
                    remoteDataSource.login(username, password, false)
                }
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return tokenUser
    }

    override suspend fun logout(accessToken: String): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.logout(accessToken)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun sendUserPublicKey(
        accessToken: String,
        userId: Int,
        userPublicKey: PublicKey,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                val publicPEMValue = RSASecure.generateRSAPEMFormat(userPublicKey, "public")
                val publicPEM = PEMData(pem = publicPEMValue)

                remoteDataSource.sendUserPublicKey(accessToken, userId, GlobalSettings.secure, publicPEM, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun haveUserRegisteredFingerprint(
        accessToken: String,
        userId: Int,
        userType: String,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        if (userType == "market") {
            return BasicResponse(
                operation = "User have a fingerprint registered",
                successful = true
            )
        }

        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.haveUserRegisteredFingerprint(accessToken, userId, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun fetchCreditsUser(
        accessToken: String,
        userId: Int,
        userPrivateKey: PrivateKey
    ): CreditList {
        val response: CreditList = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchCreditsUser(accessToken, userId, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun fetchClientProfile(
        accessToken: String,
        userId: Int,
        userPrivateKey: PrivateKey
    ): ClientProfile {
        val response: ClientProfile = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchClientProfile(accessToken, userId, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun fetchMarketProfile(
        accessToken: String,
        userId: Int,
        userPrivateKey: PrivateKey
    ): MarketProfile {
        val response: MarketProfile = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.fetchMarketProfile(accessToken, userId, GlobalSettings.secure, userPrivateKey)
            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }
}