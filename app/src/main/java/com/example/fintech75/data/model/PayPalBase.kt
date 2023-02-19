package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class PayPalBase(
    val id: String,
    val intent: String,
    val status: String,
    @SerializedName("create_time")
    val createdTime: String
)

data class AmountBase(
    @SerializedName("currency_code")
    val currencyCode: String = "MXN",
    val value: String
)

data class PayeeBase(
    @SerializedName("email_address")
    val emailAddress: String,
    @SerializedName("merchant_id")
    val merchantId: String
)

data class PayerBase(
    @SerializedName("payer_id")
    val payerId: String,
    val name: String,
    val surname: String,
    @SerializedName("email_address")
    val emailAddress: String
)

data class LinkBase(
    val href: String,
    val rel: String,
    val method: String
)

data class CaptureBase(
    val id: String,
    val status: String,
    val amount: AmountBase,
    @SerializedName("final_capture")
    val finalCapture: Boolean
)

data class PayPalOrder(
    val id: String,
    val intent: String,
    val status: String,
    @SerializedName("create_time")
    val createdTime: String,
    val total: AmountBase,
    val payee: PayeeBase,
    @SerializedName("approve_link")
    val approveLink: LinkBase,
    @SerializedName("capture_link")
    val captureLink: LinkBase
)

data class PayPalCaptureOrder(
    val id: String,
    val intent: String,
    val status: String,
    @SerializedName("create_time")
    val createdTime: String,
    val total: AmountBase,
    val payer: PayerBase,
    val capture: CaptureBase,
    @SerializedName("refund_link")
    val refundLink: LinkBase
)


