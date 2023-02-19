package com.example.fintech75.repository

import com.example.fintech75.application.AppConstants
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

class MovementRepositoryImpl(private val remoteDataSource: RemoteDataSource): MovementRepository {

    override suspend fun generateMovementSummary(
        accessToken: String,
        movementForm: MovementTypeRequest,
        typeMovement: String,
        userPrivateKey: PrivateKey
    ): MovementExtraRequest {
        val response: MovementExtraRequest = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.generateMovementSummary(
                    accessToken = accessToken,
                    movementForm = movementForm,
                    typeMovement = typeMovement,
                    secure = GlobalSettings.secure,
                    userPrivateKey = userPrivateKey
                )

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun beginMovement(
        accessToken: String,
        movementRequest: MovementExtraRequest,
        typeMovement: String,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        val response: MovementComplete = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.beginMovement(
                    accessToken = accessToken,
                    movementRequest = movementRequest,
                    typeMovement = typeMovement,
                    secure = GlobalSettings.secure,
                    userPrivateKey = userPrivateKey
                )

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun executeMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        val response: MovementComplete = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.executeMovement(accessToken, idMovement, notify, GlobalSettings.secure, userPrivateKey)

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun cancelMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse {
        val response: BasicResponse = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.cancelMovement(accessToken, idMovement, notify, GlobalSettings.secure, userPrivateKey)

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun generatePayPalOrder(
        accessToken: String,
        idMovement: Int,
        userPrivateKey: PrivateKey
    ): PayPalOrder {
        val response: PayPalOrder = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if(thereIsInternetConnection){
                remoteDataSource.generatePayPalOrder(accessToken, idMovement, GlobalSettings.secure, userPrivateKey)

            } else {
                val bodyResponse = ResponseBody.create(MediaType.parse("plain/text"), "No Internet connection available")
                throw HttpException(Response.error<ResponseBody>(400, bodyResponse))
            }
        }

        return response
    }

    override suspend fun finishPayPalMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        lateinit var processException: HttpException
        var occursError = false

        val response: MovementComplete = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if (thereIsInternetConnection) {
                // Capture PayPal Order
                withContext(Dispatchers.Default) {
                    try {
                        remoteDataSource.capturePayPalOrder(accessToken, idMovement, GlobalSettings.secure, userPrivateKey)
                    } catch (e: HttpException) {
                        processException = e
                        occursError = true
                        throw e
                    }
                }

                // Execute movement
                val createdMovement = withContext(Dispatchers.Default) {
                    if (occursError) {
                        generateEmptyMovementComplete()
                    } else {
                        try {
                            remoteDataSource.executeMovement(accessToken, idMovement, notify, GlobalSettings.secure, userPrivateKey)
                        } catch (e: HttpException) {
                            processException = e
                            occursError = true
                            throw e
                        }
                    }
                }
                createdMovement

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

    override suspend fun performAuthFingerprintMovement(
        accessToken: String,
        idMovement: Int,
        fingerprintSample: FingerprintSample,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete {
        lateinit var processException: HttpException
        var occursError = false

        val response: MovementComplete = withContext(Dispatchers.IO) {
            val thereIsInternetConnection: Boolean = withContext(Dispatchers.Default){
                InternetCheck.isNetworkAvailable()
            }

            if (thereIsInternetConnection) {
                // Save movement fingerprint
                withContext(Dispatchers.Default) {
                    try {
                        remoteDataSource.saveMovementFingerprint(
                            accessToken, idMovement, fingerprintSample, GlobalSettings.secure, userPrivateKey
                        )
                    } catch (e: HttpException) {
                        processException = e
                        occursError = true
                        throw e
                    }
                }

                // Authorize movement if fingerprint sample has been accepted
                if (!occursError) {
                    withContext(Dispatchers.Default) {
                        try {
                            remoteDataSource.authMovementFingerprint(
                                accessToken, idMovement, GlobalSettings.secure, userPrivateKey
                            )
                        } catch (e: HttpException) {
                            processException = e
                            occursError = true
                            throw e
                        }
                    }
                }

                // Execute movement
                val createdMovement = withContext(Dispatchers.Default) {
                    if (occursError) {
                        generateEmptyMovementComplete()
                    } else {
                        try {
                            remoteDataSource.executeMovement(
                                accessToken, idMovement, notify, GlobalSettings.secure, userPrivateKey
                            )
                        } catch (e: HttpException) {
                            processException = e
                            occursError = true
                            throw e
                        }
                    }
                }
                createdMovement

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

    private fun generateEmptyMovementComplete(): MovementComplete = MovementComplete(
        idCredit = AppConstants.DEFAULT_ID_CREDIT,
        idPerformer = AppConstants.DEFAULT_ID_USER,
        idRequester = AppConstants.DEFAULT_ID_CLIENT,
        typeMovement = AppConstants.DEPOSIT_MOVEMENT,
        amount = "$0.00",
        idMovement = 0,
        authorized = null,
        inProcess = false,
        successful = null,
        createdTime = "1990/01/01",
        extra = MovementExtraDetail(
            typeSubMovement = "credit",
            destinationCredit = null,
            idMarket = null,
            depositorName = null,
            paypalOrder = null,
            idDetail = "N/A",
            movementNature = "N/A"
        )
    )
}