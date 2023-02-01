package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class OwnerBase(
    @SerializedName("name_owner")
    val nameOwner: String,
    @SerializedName("type_owner")
    val typeOwner: String
)
