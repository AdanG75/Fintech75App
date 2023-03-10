package com.example.fintech75.core

import android.view.inputmethod.InputMethodManager
import java.security.PrivateKey
import java.security.PublicKey

object GlobalSettings {
    const val secure: Boolean = true

    var notify: Boolean = true
    var severPublicKey: PublicKey? = null
    var mobilePrivateKey: PrivateKey? = null
    var mobilePublicKey: PublicKey? = null
    var inputMethodManager: InputMethodManager? = null
}