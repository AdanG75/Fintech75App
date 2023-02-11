package com.example.fintech75.repository

import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.data.model.MovementExtraRequest
import com.example.fintech75.data.model.MovementTypeRequest
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
}