package com.example.fintech75.repository

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.InternetCheck
import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.MovementTypeRequest
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
}