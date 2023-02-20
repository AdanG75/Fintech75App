package com.example.fintech75.repository

import com.example.fintech75.data.model.*
import java.security.PrivateKey

interface MovementRepository {
    suspend fun generateMovementSummary(
        accessToken: String,
        movementForm: MovementTypeRequest,
        typeMovement: String,
        userPrivateKey: PrivateKey
    ): MovementExtraRequest

    suspend fun beginMovement(
        accessToken: String,
        movementRequest: MovementExtraRequest,
        typeMovement: String,
        userPrivateKey: PrivateKey
    ): MovementComplete

    suspend fun executeMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete

    suspend fun cancelMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): BasicResponse

    suspend fun generatePayPalOrder(
        accessToken: String,
        idMovement: Int,
        userPrivateKey: PrivateKey
    ): PayPalOrder

    suspend fun finishPayPalMovement(
        accessToken: String,
        idMovement: Int,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete

    suspend fun performAuthFingerprintMovement(
        accessToken: String,
        idMovement: Int,
        fingerprintSample: FingerprintSample,
        notify: Boolean,
        userPrivateKey: PrivateKey
    ): MovementComplete

    suspend fun authFingerprintMovement(
        accessToken: String,
        idMovement: Int,
        fingerprintSample: FingerprintSample,
        userPrivateKey: PrivateKey
    ): BasicResponse
}