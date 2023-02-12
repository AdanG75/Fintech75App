package com.example.fintech75.data.model

data class BasicResponse(
    val operation: String,
    val successful: Boolean
)

data class BasicDataResponse(
    val data: String
)

data class DetailMessage(
    val detail: String
)
