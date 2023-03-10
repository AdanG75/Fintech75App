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

    suspend fun fetchUserPayments(accessToken: String, userId: Int, secure: Boolean, userPrivateKey: PrivateKey): Payments {
        val response = if (secure) {
            val secureResponse = webService.secureFetchUserPayments(accessToken, userId, secure)
            CipherSecure.unpackAndDecryptData<Payments>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchUserPayments(accessToken, userId, secure)
        }

        return response
    }

    suspend fun fetchAllMarkets(accessToken: String, secure: Boolean, userPrivateKey: PrivateKey): MarketsList {
        val response = if (secure) {
            val secureResponse = webService.secureFetchAllMarkets(accessToken, secure, true)
            CipherSecure.unpackAndDecryptData<MarketsList>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchAllMarkets(accessToken, secure, true)
        }

        return response
    }

    suspend fun fetchMarketDetail(accessToken: String, idMarket: String, idUser: Int, secure: Boolean, userPrivateKey: PrivateKey) : CreditMarketClient {
        val response = if (secure) {
            val secureResponse = webService.secureFetchDetailMarket(accessToken, idMarket, idUser, secure)
            CipherSecure.unpackAndDecryptData<CreditMarketClient>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchDetailMarket(accessToken, idMarket, idUser, secure)
        }

        return response
    }

    suspend fun fetchCreditDetail(accessToken: String, idCredit: Int, secure: Boolean, userPrivateKey: PrivateKey): CreditDetail {
        val response = if (secure) {
            val secureResponse = webService.secureFetchCreditDetail(accessToken, idCredit, secure)
            CipherSecure.unpackAndDecryptData<CreditDetail>(secureResponse, userPrivateKey, null)
        } else {
            webService.fetchCreditDetail(accessToken, idCredit, secure)
        }

        return response
    }

    suspend fun generateMovementSummary(
        accessToken: String,
        movementForm: MovementTypeRequest,
        typeMovement: String,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): MovementExtraRequest {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(movementForm, false, null)
            val secureResponse = webService.secureGenerateMovementSummary(accessToken, secureData, typeMovement, secure)
            CipherSecure.unpackAndDecryptData<MovementExtraRequest>(secureResponse, userPrivateKey, null)
        } else {
            webService.generateMovementSummary(accessToken, movementForm, typeMovement, secure)
        }

        return response
    }

    suspend fun beginMovement(
        accessToken: String,
        movementRequest: MovementExtraRequest,
        typeMovement: String,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(movementRequest, false, null)
            val secureResponse = webService.secureBeginMovement(accessToken, secureData, typeMovement, secure)
            CipherSecure.unpackAndDecryptData<MovementComplete>(secureResponse, userPrivateKey, null)
        } else {
            webService.beginMovement(accessToken, movementRequest, typeMovement, secure)
        }

        return response
    }

    suspend fun createCreditOrder(
        accessToken: String,
        creditRequest: CreateCredit,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): CreditOrderResponse {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(creditRequest, false, null)
            val secureResponse = webService.secureCreateCreditOrder(accessToken, secureData, secure)
            CipherSecure.unpackAndDecryptData<CreditOrderResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.createCreditOrder(accessToken, creditRequest, secure)
        }

        return response
    }

    suspend fun saveCreditFingerprint(
        accessToken: String,
        idOrder: String,
        fingerprintSample: FingerprintSample,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(fingerprintSample, false, null)
            val secureResponse = webService.secureSaveCreditFingerprint(accessToken, idOrder, secureData, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.saveCreditFingerprint(accessToken, idOrder, fingerprintSample, secure)
        }

        return response
    }

    suspend fun authCreditOrder(
        accessToken: String,
        idOrder: String,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureResponse = webService.secureAuthCreditOrder(accessToken, idOrder, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.authCreditOrder(accessToken, idOrder, secure)
        }

        return response
    }

    suspend fun createCredit(
        accessToken: String,
        idOrder: String,
        notify: Boolean,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): CreditBase {
        val response = if (secure) {
            val secureResponse = webService.secureCreateCredit(accessToken, idOrder, notify, secure)
            CipherSecure.unpackAndDecryptData<CreditBase>(secureResponse, userPrivateKey, null)
        } else {
            webService.createCredit(accessToken, idOrder, notify, secure)
        }

        return response
    }

    suspend fun deleteCreditOrder(
        accessToken: String,
        idOrder: String,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureResponse = webService.secureDeleteCreditOrder(accessToken, idOrder, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.deleteCreditOrder(accessToken, idOrder, secure)
        }

        return response
    }

    suspend fun executeMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        val response = if (secure) {
            val secureResponse = webService.secureExecuteMovement(accessToken, idMovement, notify, secure)
            CipherSecure.unpackAndDecryptData<MovementComplete>(secureResponse, userPrivateKey, null)
        } else {
            webService.executeMovement(accessToken, idMovement, notify, secure)
        }

        return response
    }

    suspend fun cancelMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureResponse = webService.secureCancelMovement(accessToken, idMovement, notify, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.cancelMovement(accessToken, idMovement, notify, secure)
        }

        return response
    }

    suspend fun saveMovementFingerprint(
        accessToken: String,
        idMovement: Int,
        fingerprintSample: FingerprintSample,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureData = CipherSecure.packAndEncryptData(fingerprintSample, false, null)
            val secureResponse = webService.secureSaveMovementFingerprint(accessToken, idMovement, secureData, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.saveMovementFingerprint(accessToken, idMovement, fingerprintSample, secure)
        }

        return response
    }

    suspend fun authMovementFingerprint(
        accessToken: String,
        idMovement: Int,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response = if (secure) {
            val secureResponse = webService.secureAuthMovementFingerprint(accessToken, idMovement, secure)
            CipherSecure.unpackAndDecryptData<BasicResponse>(secureResponse, userPrivateKey, null)
        } else {
            webService.authMovementFingerprint(accessToken, idMovement, secure)
        }

        return response
    }

    suspend fun generatePayPalOrder(
        accessToken: String,
        idMovement: Int,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): PayPalOrder {
        val response = if (secure) {
            val secureResponse = webService.secureGeneratePayPalOrder(accessToken, idMovement, secure)
            CipherSecure.unpackAndDecryptData<PayPalOrder>(secureResponse, userPrivateKey, null)
        } else {
            webService.generatePayPalOrder(accessToken, idMovement, secure)
        }

        return response
    }

    suspend fun capturePayPalOrder(
        accessToken: String,
        idMovement: Int,
        secure: Boolean,
        userPrivateKey: PrivateKey
    ): PayPalCaptureOrder {
        val response = if (secure) {
            val secureResponse = webService.secureCapturePayPalOrder(accessToken, idMovement, secure)
            CipherSecure.unpackAndDecryptData<PayPalCaptureOrder>(secureResponse, userPrivateKey, null)
        } else {
            webService.capturePayPalOrder(accessToken, idMovement, secure)
        }

        return response
    }
}