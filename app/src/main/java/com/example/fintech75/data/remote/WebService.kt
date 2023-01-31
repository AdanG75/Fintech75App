package com.example.fintech75.data.remote

import com.example.fintech75.application.AppConstants
import com.example.fintech75.data.model.BasicResponse
import com.example.fintech75.data.model.PEMData
import com.example.fintech75.data.model.SecureBase
import com.example.fintech75.data.model.TokenBase
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface WebService {
    @GET("/public-key")
    suspend fun getPublicKeyServer():PEMData

    @FormUrlEncoded
    @POST("/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Query("secure") secure: Boolean = true
    ): TokenBase

    @DELETE("/logout")
    suspend fun logout(
        @Header("Authorization") auth: String
    ): BasicResponse

    @PUT("/public-key/{id_user}")
    suspend fun sendUserPublicKey(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false,
        @Body pemData: PEMData
    ): BasicResponse

    @PUT("/public-key/{id_user}")
    suspend fun secureSendUserPublicKey(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true,
        @Body securePEMData: SecureBase
    ): SecureBase
}

object RetrofitClient {
    val webService: WebService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(HttpClient.httpClient)
            .build()
            .create(WebService::class.java)
    }
}

object HttpClient {
    val  httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(61, TimeUnit.SECONDS)
            .readTimeout(61, TimeUnit.SECONDS)
            .build()
    }
}