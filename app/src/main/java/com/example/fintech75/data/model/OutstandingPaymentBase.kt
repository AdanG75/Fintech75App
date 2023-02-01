package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class OutstandingPaymentBase(
    @SerializedName("id_market")
    val idMarket: String,
    val amount: String,
    @SerializedName("id_outstanding")
    val idOutstanding: Int,
    @SerializedName("past_amount")
    val pastAmount: String? = null,
    @SerializedName("in_process")
    val inProcess: Boolean,
    @SerializedName("last_cash_closing")
    val lastCashClosing: String? = null,
    @SerializedName("created_time")
    val createdTime: String
)