package com.example.fintech75.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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

@Parcelize
data class MovementExtraBasicDetail(
    @SerializedName("type_submov")
    val typeSubMovement: String,
    @SerializedName("destination_credit")
    val destinationCredit: Int? = null,
    @SerializedName("id_market")
    val idMarket: String? = null,
    @SerializedName("depositor_name")
    val depositorName: String? = null,
    @SerializedName("paypal_order")
    val paypalOrder: String? = null,
    @SerializedName("depositor_email")
    val depositorEmail: String? = null
): Parcelable

data class MovementExtraDetail(
    @SerializedName("type_submov")
    val typeSubMovement: String,
    @SerializedName("destination_credit")
    val destinationCredit: Int? = null,
    @SerializedName("id_market")
    val idMarket: String? = null,
    @SerializedName("depositor_name")
    val depositorName: String? = null,
    @SerializedName("paypal_order")
    val paypalOrder: String? = null,
    @SerializedName("id_detail")
    val idDetail: String,
    @SerializedName("movement_nature")
    val movementNature: String
)

data class MovementTypeRequest(
    @SerializedName("id_credit")
    val idCredit: Int? = null,
    @SerializedName("type_movement")
    val typeMovement: String,
    val amount: Double,
    @SerializedName("type_submov")
    val typeSubMovement: String,
    @SerializedName("destination_credit")
    val destinationCredit: Int? = null,
    @SerializedName("id_market")
    val idMarket: String? = null,
    @SerializedName("depositor_name")
    val depositorName: String? = null,
    @SerializedName("depositor_email")
    val depositorEmail: String? = null,
    @SerializedName("type_transfer")
    val typeTransfer: String? = null
)

@Parcelize
data class MovementExtraRequest(
    @SerializedName("id_credit")
    val idCredit: Int? = null,
    @SerializedName("id_performer")
    val idPerformer: Int,
    @SerializedName("id_requester")
    val idRequester: String? = null,
    @SerializedName("type_movement")
    val typeMovement: String,
    val amount: String,
    @SerializedName("type_user")
    val typeUser: String,
    val extra: MovementExtraBasicDetail
): Parcelable

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
