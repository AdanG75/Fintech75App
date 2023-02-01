package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

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
)


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
    val idCredit: Int,
    @SerializedName("type_credit")
    val typeCredit: String
)


data class CreditMarketClient(
    val market: MarketComplete,
    @SerializedName("credit_client")
    val creditClient: CreditClient
)
