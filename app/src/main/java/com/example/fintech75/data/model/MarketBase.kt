package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class MarketBase(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("type_market")
    val typeMarket: String,
    @SerializedName("web_page")
    val webPage: String? = null,
    val rfc: String? = null,
    @SerializedName("id_market")
    val idMarket: String
)


data class MarketBasic(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("type_market")
    val typeMarket: String,
    @SerializedName("web_page")
    val webPage: String? = null,
    val rfc: String? = null
)


data class MarketFull(
    val market: MarketBase,
    val user: UserBasic
)


data class MarketComplete(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("type_market")
    val typeMarket: String,
    @SerializedName("web_page")
    val webPage: String? = null,
    val rfc: String? = null,
    @SerializedName("id_market")
    val idMarket: String,
    val branches: List<BranchComplete>
)


data class MarketsList(
    val markets: List<MarketFull>
)


data class MarketProfile(
    val user: UserAccount,
    val market: MarketComplete,
    val outstanding_payment: OutstandingPaymentBase
)


data class MarketRegister(
    val user: UserRegister,
    val market: MarketBasic,
    val branch: BranchRegister,
    val address: AddressRegister,
    val account: AccountRegister
)
