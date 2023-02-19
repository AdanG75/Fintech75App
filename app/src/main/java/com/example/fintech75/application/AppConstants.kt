package com.example.fintech75.application

object AppConstants {
    const val BASE_URL: String = "https://api.fintech75.app"

    const val MESSAGE_STATE_NONE = "NONE"
    const val MESSAGE_STATE_LOADING = "LOAD"
    const val MESSAGE_STATE_TRY_AGAIN = "TRY"
    const val MESSAGE_STATE_FAILURE = "FAIL"
    const val MESSAGE_STATE_FATAL_FAILURE = "FF"
    const val MESSAGE_STATE_SUCCESS = "OK"

    const val ACTION_CLOSE_SESSION = "CS"
    const val ACTION_CLOSE_APP = "CA"
    const val ACTION_TRY_AGAIN = "TA"
    const val ACTION_NONE = "NN"

    const val DEFAULT_ID_USER = 1
    const val DEFAULT_ID_CREDIT = 1
    const val DEFAULT_ID_MARKET = "MKT-b7eb707466ad4eb3b60ca153aa565036"
    const val DEFAULT_ID_CLIENT = "CLI-5966ce3b1a984854879e9dbe41f97bb0"
    const val DEFAULT_ID_BRANCH = "BRH-684572407e864236917cab16f7baa36e"

    const val PAY_MOVEMENT = "payment"
    const val DEPOSIT_MOVEMENT = "deposit"
    const val TRANSFER_MOVEMENT = "transfer"
    const val WITHDRAW_MOVEMENT = "withdraw"

    const val CASH_METHOD = "cash"
    const val CREDIT_METHOD = "credit"
    const val PAYPAL_METHOD = "paypal"

    const val AUTH_PAYPAL = "p"
    const val AUTH_FINGERPRINT = "f"
    const val AUTH_NONE = "n"
    const val AUTH_BOTH = "b"
}