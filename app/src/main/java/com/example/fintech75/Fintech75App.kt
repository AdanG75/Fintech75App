package com.example.fintech75

import android.app.Application
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment
import com.paypal.checkout.config.SettingsConfig
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.UserAction

const val PAYPAL_CLIENT_ID: String = "AcLWoxYzpeG_R-QY8QCIs7QBf_htQVqcDvwrkx3mImhyGWJM3sx8ALoBaF27b9m4M4iRmSarKgBvY0S8"

class Fintech75App: Application() {
    override fun onCreate() {
        super.onCreate()
        PayPalCheckout.setConfig(
            checkoutConfig = CheckoutConfig(
                application = this,
                clientId = PAYPAL_CLIENT_ID,
                environment = Environment.SANDBOX,
                currencyCode = CurrencyCode.MXN,
                userAction = UserAction.PAY_NOW,
                settingsConfig = SettingsConfig(
                    loggingEnabled = true,
                    shouldFailEligibility = false
                )
            )
        )
    }
}