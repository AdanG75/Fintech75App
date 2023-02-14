package com.example.fintech75.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CreditBase(
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("id_market")
    val idMarket: String,
    @SerializedName("id_account")
    val idAccount: Int,
    @SerializedName("alias_credit")
    val aliasCredit: String,
    @SerializedName("type_credit")
    val typeCredit: String,
    val amount: String,
    @SerializedName("is_approved")
    val isApproved: Boolean,
    @SerializedName("id_credit")
    val idCredit: Int,
    @SerializedName("in_process")
    val inProcess: Boolean,
    @SerializedName("created_time")
    val createdTime: String
): Parcelable


data class CreditList(
    val credits: List<CreditBase>
)


data class CreditDetail(
    val credit: CreditBase,
    val movements: List<MovementComplete>,
    val owner: OwnerBase
)


data class CreditClient(
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("have_credit")
    val haveCredit: Boolean,
    @SerializedName("id_credit")
    val idCredit: Int? = null,
    @SerializedName("type_credit")
    val typeCredit: String? = null
)


data class CreditMarketClient(
    val market: MarketComplete,
    @SerializedName("credit_client")
    val creditClient: CreditClient
)

data class CreateCredit(
    @SerializedName("id_market")
    val idMarket: String,
    @SerializedName("id_client")
    val idClient: String? = null,
    @SerializedName("client_email")
    val clientEmail: String? = null,
    @SerializedName("alias_credit")
    val aliasCredit: String,
    val amount: String
)

data class CreditSmallData(
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("id_market")
    val idMarket: String,
    @SerializedName("id_account")
    val idAccount: Int? = null,
    @SerializedName("alias_credit")
    val aliasCredit: String,
    @SerializedName("type_credit")
    val typeCredit: String,
    val amount: String,
    @SerializedName("is_approved")
    val isApproved: Boolean
)

data class CreditOwners(
    @SerializedName("market_name")
    val marketName: String,
    @SerializedName("client_name")
    val clientName: String
)

data class CreditOrderResponse(
    val credit: CreditSmallData,
    val owners: CreditOwners,
    @SerializedName("id_pre_credit")
    val idPreCredit: String
)
