package com.example.fintech75.data.remote

import com.example.fintech75.data.model.*
import com.example.fintech75.secure.CipherSecure
import java.security.PrivateKey

class RemoteDataSource(private val webService: WebService) {
    suspend fun getPublicKeyServer(): PEMData {
        return webService.getPublicKeyServer()
    }

    suspend fun login(username: String, password: String, secure: Boolean = true): TokenBase {
        return webService.login(username, password, secure = secure)
    }

    suspend fun logout(accessToken: String): BasicResponse {
        return webService.logout(auth = accessToken)
    }

    suspend fun sendUserPublicKey(accessToken: String, userId: Int, secure: Boolean, data: PEMData, userPrivateKey: PrivateKey): BasicResponse {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(data, false, null)
            val secureResponse = webService.secureSendUserPublicKey(accessToken, userId, secure, secureData)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.sendUserPublicKey(accessToken, userId, secure, data)
        }

        return response
    }

    suspend fun haveUserRegisteredFingerprint(accessToken: String, userId: Int, secure: Boolean, userPrivateKey: PrivateKey): BasicResponse {
        val response = if (secure) {
            val secureResponse = webService.secureUserHaveFingerprintRegistered(accessToken, userId, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.userHaveFingerprintRegistered(accessToken, userId, secure)
        }

        return response
    }

    suspend fun fetchCreditsUser(accessToken: String, userId: Int, secure: Boolean, userPrivateKey: PrivateKey): CreditList {
        val response = if (secure) {
            val secureResponse = webService.secureFetchUserCredits(accessToken, userId, secure)
            CipherSecure.unpackAndDecryptData<CreditList>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchUserCredits(accessToken, userId, secure)
        }

        return response
    }

    suspend fun fetchClientProfile(accessToken: String, userId: Int, secure: Boolean, userPrivateKey: PrivateKey): ClientProfile {
        val response = if (secure) {
            val secureResponse = webService.secureFetchClientProfile(accessToken, userId, secure)
            CipherSecure.unpackAndDecryptData<ClientProfile>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchClientProfile(accessToken, userId, secure)
        }

        return response
    }

    suspend fun fetchMarketProfile(accessToken: String, userId: Int, secure: Boolean, userPrivateKey: PrivateKey): MarketProfile {
        val response = if (secure) {
            val secureResponse = webService.secureFetchMarketProfile(accessToken, userId, secure)
            CipherSecure.unpackAndDecryptData<MarketProfile>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchMarketProfile(accessToken, userId, secure)
        }

        return response
    }
}