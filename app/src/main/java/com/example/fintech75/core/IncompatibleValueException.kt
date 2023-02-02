package com.example.fintech75.core

class IncompatibleValueException(
    message: String? = "Incompatible value",
    cause: Throwable? = java.lang.IllegalArgumentException("Incompatible value")
): Exception(message, cause)