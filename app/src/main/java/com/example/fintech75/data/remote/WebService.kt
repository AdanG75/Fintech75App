package com.example.fintech75.data.remote

import com.example.fintech75.application.AppConstants
import com.example.fintech75.data.model.*
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

    @GET("/fingerprint/{id_user}/have")
    suspend fun userHaveFingerprintRegistered(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @GET("/fingerprint/{id_user}/have")
    suspend fun secureUserHaveFingerprintRegistered(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/credit/user/{id_user}")
    suspend fun fetchUserCredits(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): CreditList

    @GET("/credit/user/{id_user}")
    suspend fun secureFetchUserCredits(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/user/{id_user}")
    suspend fun fetchClientProfile(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): ClientProfile

    @GET("/user/{id_user}")
    suspend fun secureFetchClientProfile(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/user/{id_user}")
    suspend fun fetchMarketProfile(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): MarketProfile

    @GET("/user/{id_user}")
    suspend fun secureFetchMarketProfile(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/movement/user/{id_user}/payments")
    suspend fun fetchUserPayments(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): Payments

    @GET("/movement/user/{id_user}/payments")
    suspend fun secureFetchUserPayments(
        @Header("Authorization") auth: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/market")
    suspend fun fetchAllMarkets(
        @Header("Authorization") auth: String,
        @Query("secure") secure: Boolean = false,
        @Query("exc_system") excludeSystem: Boolean = true
    ): MarketsList

    @GET("/market")
    suspend fun secureFetchAllMarkets(
        @Header("Authorization") auth: String,
        @Query("secure") secure: Boolean = true,
        @Query("exc_system") excludeSystem: Boolean = true
    ): SecureBase

    @GET("/market/{id_market}/client/{id_user}")
    suspend fun fetchDetailMarket(
        @Header("Authorization") auth: String,
        @Path("id_market") idMarket: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = false
    ): CreditMarketClient

    @GET("/market/{id_market}/client/{id_user}")
    suspend fun secureFetchDetailMarket(
        @Header("Authorization") auth: String,
        @Path("id_market") idMarket: String,
        @Path("id_user") idUser: Int,
        @Query("secure") secure: Boolean = true
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