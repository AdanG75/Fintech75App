package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class MovementBase(
    @SerializedName("id_credit")
    val idCredit: Int? = null,
    @SerializedName("id_performer")
    val idPerformer: Int,
    @SerializedName("id_requester")
    val idRequester: String? = null,
    @SerializedName("type_movement")
    val typeMovement: String,
    val amount: String,
    @SerializedName("id_movement")
    val idMovement: Int,
    val authorized: Boolean? = null,
    @SerializedName("in_process")
    val inProcess: Boolean? = null,
    val successful: Boolean? = null,
    @SerializedName("created_time")
    val createdTime: String
)


data class MovementExtraDetail(
    @SerializedName("type_submov")
    val typeSubMovement: String,
    @SerializedName("destination_credit")
    val destinationCredit: Int?,
    @SerializedName("id_market")
    val idMarket: String?,
    @SerializedName("depositor_name")
    val depositorName: String?,
    @SerializedName("paypal_order")
    val paypalOrder: String?,
    @SerializedName("id_detail")
    val idDetail: String,
    @SerializedName("movement_nature")
    val movementNature: String
)


data class MovementComplete(
    @SerializedName("id_credit")
    val idCredit: Int? = null,
    @SerializedName("id_performer")
    val idPerformer: Int,
    @SerializedName("id_requester")
    val idRequester: String? = null,
    @SerializedName("type_movement")
    val typeMovement: String,
    val amount: String,
    @SerializedName("id_movement")
    val idMovement: Int,
    val authorized: Boolean? = null,
    @SerializedName("in_process")
    val inProcess: Boolean? = null,
    val successful: Boolean? = null,
    @SerializedName("created_time")
    val createdTime: String,
    @SerializedName("extra")
    val extra: MovementExtraDetail
)
