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

    @GET("/credit/{id_credit}")
    suspend fun fetchCreditDetail(
        @Header("Authorization") auth: String,
        @Path("id_credit") idCredit: Int,
        @Query("secure") secure: Boolean = false
    ): CreditDetail

    @GET("/credit/{id_credit}")
    suspend fun secureFetchCreditDetail(
        @Header("Authorization") auth: String,
        @Path("id_credit") idCredit: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @POST("/movement/summary")
    suspend fun generateMovementSummary(
        @Header("Authorization") auth: String,
        @Body movementForm: MovementTypeRequest,
        @Query("type_movement") typeMovement: String,
        @Query("secure") secure: Boolean = false
    ): MovementExtraRequest

    @POST("/movement/summary")
    suspend fun secureGenerateMovementSummary(
        @Header("Authorization") auth: String,
        @Body secureMovementForm: SecureBase,
        @Query("type_movement") typeMovement: String,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @POST("/movement/type/{movement_type}")
    suspend fun beginMovement(
        @Header("Authorization") auth: String,
        @Body movementRequest: MovementExtraRequest,
        @Path("movement_type") movementType: String,
        @Query("secure") secure: Boolean = false
    ): MovementComplete

    @POST("/movement/type/{movement_type}")
    suspend fun secureBeginMovement(
        @Header("Authorization") auth: String,
        @Body secureMovementRequest: SecureBase,
        @Path("movement_type") movementType: String,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    // Movements
    @PATCH("/movement/{id_movement}/exec")
    suspend fun executeMovement(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = false
    ): MovementComplete

    @PATCH("/movement/{id_movement}/exec")
    suspend fun secureExecuteMovement(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @DELETE("/movement/{id_movement}")
    suspend fun cancelMovement(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @DELETE("/movement/{id_movement}")
    suspend fun secureCancelMovement(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @POST("/movement/{id_movement}/auth/fingerprint")
    suspend fun saveMovementFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Body fingerprintSample: FingerprintSample,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @POST("/movement/{id_movement}/auth/fingerprint")
    suspend fun secureSaveMovementFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Body secureFingerprintSample: SecureBase,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/movement/{id_movement}/auth/match")
    suspend fun authMovementFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @GET("/movement/{id_movement}/auth/match")
    suspend fun secureAuthMovementFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @PUT("/movement/{id_movement}/auth/paypal")
    suspend fun generatePayPalOrder(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = false
    ): PayPalOrder

    @PUT("/movement/{id_movement}/auth/paypal")
    suspend fun secureGeneratePayPalOrder(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/movement/{id_movement}/auth/capture")
    suspend fun capturePayPalOrder(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = false
    ): PayPalCaptureOrder

    @GET("/movement/{id_movement}/auth/capture")
    suspend fun secureCapturePayPalOrder(
        @Header("Authorization") auth: String,
        @Path("id_movement") idMovement: Int,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    // Credits
    @POST("/credit/order")
    suspend fun createCreditOrder(
        @Header("Authorization") auth: String,
        @Body creditRequest: CreateCredit,
        @Query("secure") secure: Boolean = false
    ): CreditOrderResponse

    @POST("/credit/order")
    suspend fun secureCreateCreditOrder(
        @Header("Authorization") auth: String,
        @Body secureCreditRequest: SecureBase,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @POST("/credit/order/{id_order}/fingerprint")
    suspend fun saveCreditFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Body fingerprintSample: FingerprintSample,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @POST("/credit/order/{id_order}/fingerprint")
    suspend fun secureSaveCreditFingerprint(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Body secureFingerprintSample: SecureBase,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @GET("/credit/order/{id_order}/match")
    suspend fun authCreditOrder(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @GET("/credit/order/{id_order}/match")
    suspend fun secureAuthCreditOrder(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @POST("/credit/order/{id_order}/capture")
    suspend fun createCredit(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = false
    ): CreditBase

    @POST("/credit/order/{id_order}/capture")
    suspend fun secureCreateCredit(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("notify") notify: Boolean = true,
        @Query("secure") secure: Boolean = true
    ): SecureBase

    @DELETE("/credit/order/{id_order}")
    suspend fun deleteCreditOrder(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("secure") secure: Boolean = false
    ): BasicResponse

    @DELETE("/credit/order/{id_order}")
    suspend fun secureDeleteCreditOrder(
        @Header("Authorization") auth: String,
        @Path("id_order") idOrder: String,
        @Query("secure") secure: Boolean = false
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